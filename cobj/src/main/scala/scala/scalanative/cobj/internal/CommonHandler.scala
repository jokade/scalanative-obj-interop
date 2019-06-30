package scala.scalanative.cobj.internal

import de.surfice.smacrotools.MacroAnnotationHandler

import scala.reflect.macros.{TypecheckException, whitebox}
import scala.scalanative.cobj.{CObject, CObjectWrapper, NamingConvention}

abstract class CommonHandler extends MacroAnnotationHandler {
  val c: whitebox.Context

  import c.universe._

  protected val tPtrByte = weakTypeOf[scalanative.unsafe.Ptr[Byte]]
  protected val tAnyRef = weakTypeOf[AnyRef]
  protected val tCObject = weakTypeOf[CObject]
  protected val tpeCObject = tq"$tCObject"
  protected val tCObjectWrapper = weakTypeOf[CObjectWrapper[_]]
  protected val expExtern = q"scalanative.unsafe.extern"
  protected def tpeDefaultParent: Tree

  implicit class MacroData(data: Map[String,Any]) {
    type Data = Map[String, Any]
    type Externals = Map[String, (String, Tree)]
    type Statements = Seq[Tree]

    def externalPrefix: String = data.getOrElse("externalPrefix", "").asInstanceOf[String]
    def withExternalPrefix(prefix: String): Data = data.updated("externalPrefix",prefix)

    def namingConvention: NamingConvention.Value = data.getOrElse("namingConvention",NamingConvention.SnakeCase).asInstanceOf[NamingConvention.Value]
    def withNamingConvention(nc: NamingConvention.Value): Data = data.updated("namingConvention",nc)

    def isAbstract: Boolean = data.getOrElse("isAbstract",false).asInstanceOf[Boolean]
    def withIsAbstract(flag: Boolean): Data = data.updated("isAbstract",flag)

    def constructors: Seq[(String,Seq[ValDef])] = data.getOrElse("constructors",Nil).asInstanceOf[Seq[(String,Seq[ValDef])]]
    def withConstructors(ctors: Seq[(String,Seq[Tree])]): Data = data.updated("constructors",ctors)

    def externals: Externals = data.getOrElse("externals", Map()).asInstanceOf[Externals]
    def withExternals(externals: Externals): Data = data.updated("externals",externals)

    def currentType: String = data.getOrElse("currentType","").asInstanceOf[String]
    def withCurrentType(tpe: String): Data = data.updated("currentType",tpe)

    def parentIsCObj: Boolean = data.getOrElse("parentIsCObj",false).asInstanceOf[Boolean]
    def withParentIsCObj(flag: Boolean): Data = data.updated("parentIsCObj",flag)

    def additionalCompanionStmts: Statements = data.getOrElse("compStmts", Nil).asInstanceOf[Statements]
    def withAdditionalCompanionStmts(stmts: Statements): Data = data.updated("compStmts",stmts)
  }

  // TODO: move all checks (isCRef, isAbstract, ... in here and store the results in data)
  protected def analyzeTypes(tpe: TypeParts)(data: Data): Data = {
    val isAbstract = tpe.modifiers.hasFlag(Flag.ABSTRACT)
    val parentIsCObj = isExternalObject(tpe.parents.head,data)
    data
      .withIsAbstract(isAbstract)
      .withCurrentType(tpe.fullName)
      .withParentIsCObj(parentIsCObj)
  }

  protected def analyzeBody(tpe: CommonParts)(data: Data): Data = {
    val prefix = data.externalPrefix
    val typeExternals = tpe.body.collect {
      case t@DefDef(mods, name, types, args, rettype, rhs) if isExtern(rhs) =>
        genExternalBinding(prefix,t,!tpe.isObject)(data)
    }
    val companionExternals = tpe match {
      case t: TypeParts => t.companion.map(_.body.collect {
        case t@DefDef(mods, name, types, args, rettype, rhs) if isExtern(rhs) =>
          genExternalBinding(prefix,t,false)(data)
      }).getOrElse(Map())
      case _ => Nil
    }

    data.withExternals( (typeExternals ++ companionExternals).toMap )
  }

  protected def genTransformedCtorParams(cls: ClassTransformData): Seq[Tree] =
    if(cls.data.parentIsCObj)
      Seq(q"override val __ptr: $tPtrByte")
    else
      Seq(q"val __ptr: $tPtrByte")

  protected def genTransformedParents(cls: TypeTransformData[TypeParts]): Seq[Tree] = {
    cls.modParts.parents map (p => (p,getType(p,true))) map {
      case (tree,tpe) if tpe =:= tAnyRef => tpeDefaultParent
      case (tree,tpe) if tpe =:= tCObject || tpe.typeSymbol.isAbstract => tree
      case (tree,tpe) if tpe <:< tCObject => q"$tree(__ptr)"
      case (tree,_) => tree
    }
  }

  protected def genTransformedTypeBody(t: TypeTransformData[TypeParts]): Seq[Tree] = {
    val companion = t.modParts.companion.get.name
    val imports = Seq(q"import $companion.__ext")
    val ctors = genSecondaryConstructor(t)
    //        if(isMutable)
    //          Nil
    //        else if(isAbstract(t))
    //          Seq(q"def __ref: scalanative.native.cobj.Ref[${t.data.crefType}]")
    //        else
    //          genSecondaryConstructor(t)
    //      val ctors = Seq(s"val __ref: ${t.data.crefType} = ")
    imports ++ ctors ++ (t.modParts.body map {
      case tree @ DefDef(mods, name, types, args, rettype, rhs) if isExtern(rhs) =>
        val externalKey = genScalaName(tree)(t.data)
        val externalName = t.data.externals(externalKey)._1
        genExternalCall(externalName,tree,false,t.data)
      case default => default
    })
  }

  protected def transformExternalBindingParams(params: List[ValDef], data: Data, outParams: Boolean = false): List[ValDef] = {
    params map {
      case ValDef(mods,name,tpt,rhs) if isExternalObject(tpt,data) =>
        ValDef(mods,name,q"$tPtrByte",rhs)
      case default => default
    }
  }

  protected def genTransformedCompanionBody(t: TransformData[CommonParts]): Seq[Tree] = {
    t.modParts.body map {
      case tree @ DefDef(mods, name, types, args, rettype, rhs) if isExtern(rhs) =>
        val externalKey = genScalaName(tree)(t.data)
        val externalName = t.data.externals(externalKey)._1
        genExternalCall(externalName,tree,t.modParts.isObject,t.data)
      case default => default
    }
  }

  private def genSecondaryConstructor(t: TypeTransformData[TypeParts]): Seq[Tree] = {
    val companion = t.modParts.companion.get.name
    t.data.constructors.map { p =>
      val args = transformExternalCallArgs(p._2,t.data)
      DefDef(Modifiers(),
        termNames.CONSTRUCTOR,
        List(),
        List(p._2.toList),
        TypeTree(),
        Block(
          Nil,
          Apply(Ident(termNames.CONSTRUCTOR), List(q"$companion.__ext.${TermName(p._1)}(..$args)"))
        ))
    }
  }

  protected def genBindingsObject(data: MacroData): Tree = {
    val ctors = data.constructors.map{
      case (externalName,args) => q"def ${TermName(externalName)}(..$args): $tPtrByte = $expExtern"
    }
    val defs = data.externals.values.map(_._2)
    q"""@scalanative.unsafe.extern object __ext {..${ctors++defs}}"""
  }

  protected def genExternalCall(externalName: String, scalaDef: DefDef, isClassMethod: Boolean, data: Data): DefDef = {
    import scalaDef._
    val (args,wrappers) = vparamss match {
      case Nil => (None,Nil)
      case List(args) => (Some(transformExternalCallArgs(args,data)),Nil)
      case List(inargs,outargs) =>
        val (wrappers,filteredOutargs) = outargs.partition(p => isCObjectWrapper(p.tpt))
        val wrapperName = wrappers.headOption.map(_.name)
        (Some( transformExternalCallArgs(inargs,data,false,wrappers) ++ transformExternalCallArgs(filteredOutargs,data,false,wrappers) ), wrappers)
      case _ =>
        c.error(c.enclosingPosition,"extern methods with more than two parameter lists are not supported for @CObj classes")
        ???
    }
    val external = TermName(externalName)
    val call = args match {
      case Some(as) if isClassMethod => q"__ext.$external(..$as)"
      case Some(as) => q"__ext.$external(__ptr,..$as)"
      case None if isClassMethod => q"__ext.$external"
      case None => q"__ext.$external(__ptr)"
    }
    val rhs = wrapExternalCallResult(call,tpt,data,nullable(scalaDef),returnsThis(scalaDef),wrappers)

    DefDef(mods,name,tparams,vparamss,tpt,rhs)
  }

  protected def transformExternalCallArgs(args: Seq[ValDef], data: Data, outArgs: Boolean = false, wrappers: List[ValDef] = Nil): Seq[Tree] = {
    args map {
      case ValDef(_,name,tpt,_) if isExternalObject(tpt,data) || outArgs =>
        findWrapper(tpt,wrappers) match {
          case Some(wrapperName) => q"""$wrapperName.unwrap($name)"""
          case _ => q"""if($name==null) null else $name.__ptr"""
        }
      case ValDef(_,name,AppliedTypeTree(tpe,_),_) if tpe.toString == "_root_.scala.<repeated>" => q"$name:_*"
      case ValDef(_,name,tpt,_) => q"$name"
    }
  }

  protected def genExternalBinding(prefix: String, scalaDef: DefDef, isInstanceMethod: Boolean)(implicit data: Data): (String,(String,Tree)) = {
    val scalaName = genScalaName(scalaDef)
    val externalName = genExternalName(prefix,scalaDef)
    val externalParams =
      if(isInstanceMethod) scalaDef.vparamss match {
        case Nil => List(List(q"val self: $tPtrByte"))
        case List(params) => List(q"val self: $tPtrByte" +: transformExternalBindingParams(params,data) )
        case List(inparams,outparams) =>
          val filteredOutparams = outparams.filter(p => !isCObjectWrapper(p.tpt) )
          List( q"val self: $tPtrByte" +:
            (transformExternalBindingParams(inparams,data) ++ transformExternalBindingParams(filteredOutparams,data,true)) )
        case _ =>
          c.error(c.enclosingPosition,"extern methods with more than two parameter lists are not supported for @CObj classes")
          ???
      }
      else scalaDef.vparamss match {
        case Nil => List(Nil)
        case List(params) => List(transformExternalBindingParams(params,data))
        case List(inparams,outparams) => List(transformExternalBindingParams(inparams++outparams,data))
        case x =>
          c.error(c.enclosingPosition,"extern methods with more than two parameter lists are not supported for @CObj classes")
          ???
      }
    val tpt =
      if(isExternalObject(scalaDef.tpt,data)) tq"scalanative.unsafe.Ptr[Byte]"
      else scalaDef.tpt
    val mods = Modifiers(NoFlags,scalaDef.mods.privateWithin,scalaDef.mods.annotations) // remove flags (e.g. 'override')
    val externalDef = DefDef(mods,TermName(externalName),scalaDef.tparams,externalParams,tpt,scalaDef.rhs)

    (scalaName,(externalName,externalDef))
  }

  protected def genExternalName(prefix: String, scalaDef: DefDef)(implicit data: Data): String = {
    val scalaName = genScalaName(scalaDef)
    data.namingConvention match {
      case NamingConvention.SnakeCase =>
        prefix + scalaName.replaceAll("([A-Z])","_$1").toLowerCase
      case NamingConvention.PascalCase =>
        prefix + scalaName.head.toUpper + scalaName.tail
      case _ => prefix + scalaName
    }
  }

  protected def genScalaName(scalaDef: DefDef)(implicit data: Data): String = data.namingConvention match {
    case NamingConvention.CxxWrapper =>
      scalaDef.name.toString + (scalaDef.vparamss match {
          case Nil => ""
          case List(params) => "_" + params.map(_.tpt).mkString.hashCode
        })
    case _ =>
      scalaDef.name.toString
  }

  protected def wrapExternalCallResult(tree: Tree, tpt: Tree, data: Data, isNullable: Boolean, returnsThis: Boolean, wrappers: List[ValDef] = Nil): Tree =
    findWrapper(tpt,wrappers) match {
      case Some(wrapperName) =>
        if(isNullable)
          q"""val res = $tree; if(res == null) null.asInstanceOf[$tpt] else $wrapperName.wrap(res)"""
        else
          q"$wrapperName.wrap($tree)"
      case None if isExternalObject(tpt,data) =>
        if(returnsThis)
          q"""$tree;this"""
        else if(isNullable)
          q"""val res = $tree; if(res == null) null else new $tpt(res)"""
        else
          q"""new $tpt($tree)"""
      case _ => tree
    }

  protected def findWrapper(tpt: Tree, wrappers: List[ValDef]): Option[TermName] =
    if(wrappers.isEmpty) None
    else {
      wrappers.find {
        case ValDef(_,_,AppliedTypeTree(_,List(Ident(n))),_) if n.toString == tpt.toString => true
        case _ => false
      }.map(_.name)
    }

  protected def isExtern(rhs: Tree): Boolean = rhs match {
    case Ident(TermName(id)) =>
      id == "extern"
    case Select(_,name) =>
      name.toString == "extern"
    case x =>
      false
  }

  protected def isExternalObject(tpt: Tree, data: Data): Boolean =
    try {
      val typed = getType(tpt,true)
      // TODO: do we still need the check for tCRef (or can we only check for tCObjWrapper)
      typed.baseClasses.map(_.asType.toType).exists( t => t <:< tCObject) ||
        this.findAnnotation(typed.typeSymbol,"scalanative.cobj.CObj.CObjWrapper").isDefined ||
        data.currentType == getQualifiedTypeName(tpt,true).split('[').head // we're splitting at '[' to handle types with type parameters
    } catch {
      case ex: TypecheckException => true // the type check fails for type parameters => we assume that they represent an external Object
    }

  protected def isCObjectWrapper(tpt: Tree): Boolean =
    getType(tpt,true) <:< tCObjectWrapper

  protected def nullable(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.nullable").isDefined

  protected def returnsThis(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.returnsThis").isDefined
}
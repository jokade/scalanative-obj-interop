package scala.scalanative.cobj.internal

import de.surfice.smacrotools.MacroAnnotationHandler

import scala.reflect.macros.{TypecheckException, whitebox}
import scala.scalanative.cobj.{CEnum, CObject, CObjectWrapper, NamingConvention, ResultPtr, ResultValue}
import scala.scalanative.unsafe.{CLongLong, CString, CUnsignedChar, CUnsignedLongLong, Ptr}
import scala.scalanative.unsigned.{UByte, UInt, ULong, UShort}

abstract class CommonHandler extends MacroAnnotationHandler {
  val c: whitebox.Context

  import c.universe._

  type External = (String,(String,Tree))

  protected val tRawPtr = weakTypeOf[scalanative.runtime.RawPtr]
  protected val tpeRawPtr = tq"$tRawPtr"
  protected val tPtrByte = weakTypeOf[scalanative.unsafe.Ptr[Byte]]
  protected val tpePtrByte = tq"$tPtrByte"
  protected val tAnyRef = weakTypeOf[AnyRef]
  protected val tCObject = weakTypeOf[CObject]
  protected val tpeCObject = tq"$tCObject"
  protected val tCObjectWrapper = weakTypeOf[CObjectWrapper[_]]
  protected val tCEnum = weakTypeOf[CEnum#Value]
  protected val tResultPtr = weakTypeOf[ResultPtr[_]]
  protected val tResultValue = weakTypeOf[ResultValue[_]]
  protected val expExtern = q"scalanative.unsafe.extern"

  protected val tFloat = c.weakTypeOf[Float]
  protected val tpeFloat = tq"$tFloat"
  protected val tDouble = c.weakTypeOf[Double]
  protected val tpeDouble = tq"$tDouble"
  protected val tBoolean = c.weakTypeOf[Boolean]
  protected val tpeBoolean = tq"$tBoolean"
  protected val tByte = c.weakTypeOf[Byte]
  protected val tpeByte = tq"$tByte"
  protected val tUByte = c.weakTypeOf[UByte]
  protected val tpeUByte = tq"$tUByte"
  protected val tShort = c.weakTypeOf[Short]
  protected val tpeShort = tq"$tShort"
  protected val tUShort = c.weakTypeOf[UShort]
  protected val tpeUShort = tq"$tUShort"
  protected val tInt = c.weakTypeOf[Int]
  protected val tpeInt = tq"$tInt"
  protected val tUInt = c.weakTypeOf[UInt]
  protected val tpeUInt = tq"$tUInt"
  protected val tLong = c.weakTypeOf[Long]
  protected val tpeLong = tq"$tLong"
  protected val tULong = c.weakTypeOf[ULong]
  protected val tpeULong = tq"$tULong"
  protected val tUnit = c.weakTypeOf[Unit]
  protected val tpeUnit = tq"$tUnit"
  protected val tPtr = c.weakTypeOf[Ptr[_]]
  protected val tpePtr = tq"$tPtr"
  protected val tChar = c.weakTypeOf[Char]
  protected val tpeChar = tq"$tChar"
  protected val tUChar = c.weakTypeOf[CUnsignedChar]
  protected val tpeUChar = tq"$tUChar"
  protected val tLongLong = c.weakTypeOf[CLongLong]
  protected val tpeLongLong = tq"$tLongLong"
  protected val tULongLong = c.weakTypeOf[CUnsignedLongLong]
  protected val tpeULongLong = tq"$tULongLong"
  protected val tCString = c.weakTypeOf[CString]
  protected val tpeCString = tq"$tCString"
  protected val tAnyVal = c.weakTypeOf[AnyVal]

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
//    def withExternals(externals: Externals): Data = data.updated("externals",externals)
    def addExternals(externals: Iterable[(String,(String,Tree))]): Data = data.updated("externals",data.externals ++ externals)

    def currentType: String = data.getOrElse("currentType","").asInstanceOf[String]
    def withCurrentType(tpe: String): Data = data.updated("currentType",tpe)

    def parentIsCObj: Boolean = data.getOrElse("parentIsCObj",false).asInstanceOf[Boolean]
    def withParentIsCObj(flag: Boolean): Data = data.updated("parentIsCObj",flag)

    def requiresPtrImpl: Boolean = data.getOrElse("requiresPtrImpl",true).asInstanceOf[Boolean]
    def withRequiresPtrImpl(flag: Boolean): Data = data.updated("requiresPtrImpl",flag)

    def additionalCompanionStmts: Statements = data.getOrElse("compStmts", Nil).asInstanceOf[Statements]
    //def withAdditionalCompanionStmts(stmts: Statements): Data = data.updated("compStmts",stmts)
    def addCompanionStmts(stmts: Statements): Data = data.updated("compStmts", additionalCompanionStmts ++ stmts)
  }

  // TODO: move all checks (isCRef, isAbstract, ... in here and store the results in data)
  protected def analyzeTypes(tpe: TypeParts)(data: Data): Data = {
    val isAbstract = tpe.modifiers.hasFlag(Flag.ABSTRACT)
    val parentIsCObj = isExternalObject(tpe.parents.head,data)
    val ptrImpl = ! hasPtrImpl(tpe.parents.head)
    data
      .withIsAbstract(isAbstract)
      .withCurrentType(tpe.fullName)
      .withParentIsCObj(parentIsCObj)
      .withRequiresPtrImpl(ptrImpl)
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

    data.addExternals( (typeExternals ++ companionExternals).toMap )
  }

  protected def genTransformedCtorParams(cls: ClassTransformData): (Seq[Tree],Seq[Tree]) =
    if(cls.data.requiresPtrImpl)
      (Seq(q"var __ptr: $tPtrByte"),cls.modParts.params)
    else
      (Seq(q"private val ptr: $tPtrByte"),cls.modParts.params)

  protected def genTransformedParents(cls: TypeTransformData[TypeParts]): Seq[Tree] = {
    cls.modParts.parents map (p => (p,getType(p,true))) map {
      case (tree,tpe) if tpe =:= tAnyRef =>
        c.warning(c.enclosingPosition,s"${cls.modParts.fullName} doesn't extend $tpeDefaultParent! This might result in unexpected runtime behaviour!")
        tpeDefaultParent
      case (tree,tpe) if tpe =:= tCObject || tpe.typeSymbol.asClass.isTrait => tree
      case (tree,tpe) if tpe <:< tCObject =>
        if(cls.data.requiresPtrImpl) q"$tree" else q"$tree(ptr)"
      case (tree,_) => tree
    }
  }

  private def isTrait(tpe: Type): Boolean = tpe.typeSymbol.asClass.isTrait

  protected def genTransformedTypeBody(t: TypeTransformData[TypeParts]): Seq[Tree] = {
    val companion = t.modParts.companion.get.name
    val imports = Seq(q"import $companion.__ext")
    val ctors = genSecondaryConstructor(t)
    val ptrAssign = t match {
      case cls: ClassTransformData =>
        if(!cls.data.requiresPtrImpl) Seq(q"this.__ptr = ptr")
        else Nil
      case _ => Nil
    }

    imports ++ ctors ++ ptrAssign ++ transformBody(t.modParts.body)(t.data)
  }

  protected def transformBody(body: Seq[Tree])(implicit data: Data): Seq[Tree] =
    body map {
      case scalaDef @ DefDef(_, _, _, _, _, rhs) if isExtern(rhs) =>
        transformBody(scalaDef)
      case default => default
    }

  protected def transformBody(scalaDef: DefDef)(implicit data: Data): Tree = {
    val externalKey = genScalaName(scalaDef)(data)
    val externalName = data.externals(externalKey)._1
    genExternalCall(externalName, scalaDef, false, data)
  }

  protected def implicitParamsFilter(param: ValDef): Boolean =
    !param.mods.hasFlag(Flag.IMPLICIT) || (getType(param.tpt) match {
      case t if t <:< tCObject => true
//      case t if (t <:< tResultPtr || t <:< tResultValue) => true
      case _ => false
    })

  protected def transformExternalBindingParams(params: List[ValDef], data: Data): List[ValDef] = {
    params filter(implicitParamsFilter) map {
      case ValDef(mods,name,tpt,rhs) if isExternalObject(tpt,data) =>
        ValDef(mods,name,q"$tPtrByte",rhs)
      case ValDef(mods,name,tpt,rhs) if isCEnum(tpt,data) =>
        ValDef(mods,name,tpeInt,rhs)
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
    q"""@scalanative.unsafe.extern object __ext {import scalanative.unsafe._; ..${ctors++defs}}"""
  }

  protected def genExternalCall(externalName: String, scalaDef: DefDef, isClassMethod: Boolean, data: Data): DefDef = {
    import scalaDef._
    val (args,wrappers) = vparamss match {
      case Nil => (None,Nil)
      case List(args) => (Some(transformExternalCallArgs(args,data)),Nil)
      case List(inargs,outargs) =>
        val (wrappers,filteredOutargs) = outargs.partition(p => isCObjectWrapper(p.tpt))
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
    args filter(implicitParamsFilter) map {
      case ValDef(_,name,tpt,_) if isExternalObject(tpt,data) || outArgs =>
        findWrapper(tpt,wrappers) match {
          case Some(wrapperName) => q"""$wrapperName.unwrap($name)"""
          case _ => q"""if($name==null) null else $name.__ptr"""
        }
      case ValDef(_,name,tpt,_) if isCEnum(tpt,data) =>
        q"$name.value"
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
            (transformExternalBindingParams(inparams,data) ++ transformExternalBindingParams(filteredOutparams,data)) )
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
      else if(isCEnum(scalaDef.tpt,data)) tpeInt
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
      case NamingConvention.LowerCase =>
        prefix + scalaName.toLowerCase
      case _ => prefix + scalaName
    }
  }

  protected def genScalaName(scalaDef: DefDef)(implicit data: Data): String = data.namingConvention match {
    case NamingConvention.CxxWrapper =>
      scalaDef.name.toString + (scalaDef.vparamss match {
          case Nil => ""
          case List(params) => "_" + params.filter(implicitParamsFilter).map(_.tpt).mkString.hashCode.abs
          case List(inargs,outargs) => "_" + (inargs++outargs).filter(implicitParamsFilter).map(_.tpt).mkString.hashCode.abs
          case _ =>
            c.error(c.enclosingPosition,"external bindings with more than two parameter lists ar enot supported")
            ???
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
        q"""this.__ptr = $tree
            this"""
        else if(isNullable)
          q"""val res = $tree; if(res == null) null else new $tpt(res)"""
        else
          q"""new $tpt($tree)"""
      case None if isCEnum(tpt,data) =>
        q"new $tpt($tree)"
      case _ => tree
    }

  protected def findWrapper(tpt: Tree, wrappers: List[ValDef]): Option[TermName] =
    if(wrappers.isEmpty) None
    else {
      wrappers.find {
//        case ValDef(_,_,AppliedTypeTree(_,List(Ident(n))),_) if n.toString == tpt.toString => true
        case ValDef(_,_,AppliedTypeTree(_,List(x)),_) if x.toString() == tpt.toString() => true
        case _ => false
      }.map(_.name)
    }

  protected def genWrapperImplicit(tpe: TypeName, tparams: Seq[Tree], params: Seq[Tree]): Tree = {
    val implicits = params.collect{
      case v: ValDef if !implicitParamsFilter(v) => v
    }
    tparams.size match {
      case 0 =>
        implicits match {
          case Nil =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper[$tpe] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte]) = new $tpe(ptr)
                  def unwrap(value: $tpe): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }
             """
          case Seq(arg1) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper1[$tpe,${arg1.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1) = new $tpe(ptr)
                  def unwrap(value: $tpe): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }
             """
          case Seq(arg1,arg2) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper2[$tpe,${arg1.tpt},${arg2.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2) = new $tpe(ptr)
                  def unwrap(value: $tpe): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }
             """
          case Seq(arg1,arg2,arg3) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper3[$tpe,${arg1.tpt},${arg2.tpt},${arg3.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3) = new $tpe(ptr)
                  def unwrap(value: $tpe): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }
             """
          case Seq(arg1,arg2,arg3,arg4) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper4[$tpe,${arg1.tpt},${arg2.tpt},${arg3.tpt},${arg4.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3,$arg4) = new $tpe(ptr)
                  def unwrap(value: $tpe): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }
             """
          case _ =>
            c.error(c.enclosingPosition,"constructors with more than 4 implicit parameters are not supported")
            ???
        }
      case 1 =>
        implicits match {
          case Nil =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper[$tpe[_]] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte]) = new $tpe(ptr)
                  def unwrap(value: $tpe[_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper1[$tpe[_],${arg1.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1) = new $tpe(ptr)
                  def unwrap(value: $tpe[_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper2[$tpe[_],${arg1.tpt},${arg2.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2) = new $tpe(ptr)
                  def unwrap(value: $tpe[_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2,arg3) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper3[$tpe[_],${arg1.tpt},${arg2.tpt},${arg3.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3) = new $tpe(ptr)
                  def unwrap(value: $tpe[_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2,arg3,arg4) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper4[$tpe[_],${arg1.tpt},${arg2.tpt},${arg3.tpt},${arg4.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3,$arg4) = new $tpe(ptr)
                  def unwrap(value: $tpe[_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case _ =>
            c.error(c.enclosingPosition,"constructors with more than 4 implicit parameters are not supported")
            ???
        }
      case 2 =>
        implicits match {
          case Nil =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper[$tpe[_,_]] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte]) = new $tpe(ptr)
                  def unwrap(value: $tpe[_,_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper1[$tpe[_,_],${arg1.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1) = new $tpe(ptr)
                  def unwrap(value: $tpe[_,_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper2[$tpe[_,_],${arg1.tpt},${arg2.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2) = new $tpe(ptr)
                  def unwrap(value: $tpe[_,_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2,arg3) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper3[$tpe[_,_],${arg1.tpt},${arg2.tpt},${arg3.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3) = new $tpe(ptr)
                  def unwrap(value: $tpe[_,_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case Seq(arg1,arg2,arg3,arg4) =>
            q"""implicit object __wrapper extends scalanative.cobj.CObjectWrapper4[$tpe[_,_],${arg1.tpt},${arg2.tpt},${arg3.tpt},${arg4.tpt}] {
                  def wrap(ptr: scalanative.unsafe.Ptr[Byte])($arg1,$arg2,$arg3,$arg4) = new $tpe(ptr)
                  def unwrap(value: $tpe[_,_]): scalanative.unsafe.Ptr[Byte] = value.__ptr
                }"""
          case _ =>
            c.error(c.enclosingPosition,"constructors with more than 4 implicit parameters are not supported")
            ???
        }
      case _ =>
        c.error(c.enclosingPosition,"CObj / Cxx classes with more than two type parameters are not supported")
        ???
    }
  }

  protected def genCFuncPtr(termName: TermName, params: List[ValDef], resultType: Type): Tree = {
    val argTypes = params.map( _.tpe )
    val argNames = params.map( _.name )
    params.length match {
      case 0 => q"new scalanative.unsafe.CFuncPtr0[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 1 => q"new scalanative.unsafe.CFuncPtr1[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 2 => q"new scalanative.unsafe.CFuncPtr2[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 3 => q"new scalanative.unsafe.CFuncPtr3[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 4 => q"new scalanative.unsafe.CFuncPtr4[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 5 => q"new scalanative.unsafe.CFuncPtr5[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 6 => q"new scalanative.unsafe.CFuncPtr6[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 7 => q"new scalanative.unsafe.CFuncPtr7[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 8 => q"new scalanative.unsafe.CFuncPtr8[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case 9 => q"new scalanative.unsafe.CFuncPtr9[..$argTypes,$resultType] { def apply(..$params) = $termName(..$argNames) }"
      case x =>
        c.error(c.enclosingPosition,s"function pointers with $x arguments are not supported")
        ???
    }
  }

  protected def isExtern(rhs: Tree): Boolean = rhs match {
    case Ident(TermName(id)) =>
      id == "extern"
    case Select(_,name) =>
      name.toString == "extern"
    case x =>
      false
  }

  private val namePtr = TermName("__ptr")

  protected def hasPtrImpl(tpt: Tree): Boolean =
    try {
      val typed = getType(tpt,true)
      ! typed.member(namePtr).isAbstract
    } catch {
      case ex: TypecheckException => true // the type check fails for type parameters => we assume that they represent an external Object
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

  protected def isCEnum(tpt: Tree, data: Data): Boolean =
    try {
      val typed = getType(tpt,true)
      // TODO: do we still need the check for tCRef (or can we only check for tCObjWrapper)
      typed.baseClasses.map(_.asType.toType).exists( t => t <:< tCEnum)
    } catch {
      case ex: TypecheckException => false // if the type check fails we assume that the type isn't an enum
    }

  private val _nameAnnotationArgs = Seq("name")
  protected def nameAnnotation(m: DefDef): Option[String] =
    findAnnotation(m.mods.annotations,"scala.scalanative.unsafe.name")
      .map{ p =>
        extractAnnotationParameters(p,_nameAnnotationArgs).apply("name").flatMap(extractStringConstant).get
      }

  protected def nullable(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.nullable").isDefined

  protected def returnsThis(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.returnsThis").isDefined

  protected def returnsValue(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.returnsValue").isDefined

  protected def executeSyncOnMainThread(m: DefDef): Boolean =
    findAnnotation(m.mods.annotations,"scala.scalanative.cobj.syncOnMainThread").isDefined


  object ArgsAndWrappers {
    def unapply(argLists: List[List[ValDef]]): Option[(Option[List[ValDef]],List[ValDef])] = argLists match {
      case Nil => Some((None,Nil))
      case List(args) => Some( (Some(args),Nil) )
      case List(inargs,outargs) =>
        val (wrappers,filteredOutargs) = outargs.partition(p => isCObjectWrapper(p.tpt))
        Some( (Some(inargs++filteredOutargs), wrappers) )
      case _ =>
        None
    }
  }
}

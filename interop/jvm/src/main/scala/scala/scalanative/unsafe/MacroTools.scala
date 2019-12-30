// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 1)
package scala.scalanative.unsafe

import scala.annotation.tailrec
import scala.reflect.macros.blackbox

protected[unsafe] trait MacroTools {
  val c: blackbox.Context

  import c.universe._

  val tCChar        = c.weakTypeOf[CChar]
  val tCShort       = c.weakTypeOf[CShort]
  val tCInt         = c.weakTypeOf[CInt]
  val tCLong        = c.weakTypeOf[CLong]
  val tCLongLong    = c.weakTypeOf[CLongLong]
  val tCUnsignedInt = c.weakTypeOf[CUnsignedInt]
  val tCFloat       = c.weakTypeOf[CFloat]
  val tCDouble      = c.weakTypeOf[CDouble]
  val tPtrByte      = c.weakTypeOf[Ptr[Byte]]
  val tCStruct      = c.weakTypeOf[CStruct]

  def computeFieldSize(tpe: c.Type): Int = tpe match {
    case t if t =:= tCChar        => 1
    case t if t =:= tCShort       => 2
    case t if t =:= tCInt         => 4
    case t if t =:= tCLong        => 8
    case t if t =:= tCLongLong    => 8
    case t if t =:= tCUnsignedInt => 4
    case t if t =:= tCFloat       => 4
    case t if t =:= tCDouble      => 8
    case t if t =:= tPtrByte      => 8
    case t if t <:< tCStruct      =>
      t.dealias.typeArgs.foldLeft(0)((size,tpe)=> size + computeFieldSize(tpe))
  }

  def computeFieldOffset(typeArgs: List[c.Type], index: Int): Int = {
    @tailrec
    def loop(lastOffset: Int, fields: List[c.Type]): Int = fields match {
      case Nil => lastOffset
      case x :: Nil => lastOffset
      case x :: y :: xs =>
        val newOffset = lastOffset + computeFieldSize(x)
        val nextFieldSize = computeFieldSize(y)
        val padding = (nextFieldSize - (newOffset % nextFieldSize)) % nextFieldSize
        //        println(s"newOffset: $newOffset    nextFieldSize: $nextFieldSize    padding: $padding")
        loop(newOffset + padding, y::xs)
    }
    loop(0,typeArgs.take(index+1))
  }

  def genStructInstantiation(typeArgs: List[c.Type], rawptr: c.Tree): c.Tree = typeArgs match {
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1) => q"new scalanative.unsafe.CStruct1[$t1]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2) => q"new scalanative.unsafe.CStruct2[$t1, $t2]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3) => q"new scalanative.unsafe.CStruct3[$t1, $t2, $t3]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4) => q"new scalanative.unsafe.CStruct4[$t1, $t2, $t3, $t4]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5) => q"new scalanative.unsafe.CStruct5[$t1, $t2, $t3, $t4, $t5]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6) => q"new scalanative.unsafe.CStruct6[$t1, $t2, $t3, $t4, $t5, $t6]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7) => q"new scalanative.unsafe.CStruct7[$t1, $t2, $t3, $t4, $t5, $t6, $t7]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8) => q"new scalanative.unsafe.CStruct8[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9) => q"new scalanative.unsafe.CStruct9[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) => q"new scalanative.unsafe.CStruct10[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) => q"new scalanative.unsafe.CStruct11[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12) => q"new scalanative.unsafe.CStruct12[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13) => q"new scalanative.unsafe.CStruct13[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14) => q"new scalanative.unsafe.CStruct14[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15) => q"new scalanative.unsafe.CStruct15[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16) => q"new scalanative.unsafe.CStruct16[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17) => q"new scalanative.unsafe.CStruct17[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18) => q"new scalanative.unsafe.CStruct18[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19) => q"new scalanative.unsafe.CStruct19[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20) => q"new scalanative.unsafe.CStruct20[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19, $t20]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 55)
    case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21) => q"new scalanative.unsafe.CStruct21[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19, $t20, $t21]($rawptr)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 57)
  }

  def genGetValue(prefix: c.Expr[c.PrefixType], tpe: Type, offset: Int): Tree = {
    tpe match {
      case t if t =:= tCChar        => q"$prefix.raw.getByte($offset)"
      case t if t =:= tCShort       => q"$prefix.raw.getShort($offset)"
      case t if t =:= tCInt         => q"$prefix.raw.getInt($offset)"
      case t if t =:= tCLong        => q"$prefix.raw.getNativeLong($offset)"
      case t if t =:= tCLongLong    => q"$prefix.raw.getLong($offset)"
      case t if t =:= tCUnsignedInt => q"scalanative.unsigned.UInt.fromInt($prefix.raw.getInt($offset))"
      case t if t =:= tPtrByte      => q"new scalanative.unsafe.Ptr[Byte]($prefix.raw.getPointer($offset))"
      case t if t <:< tCStruct      => genStructInstantiation(t.typeArgs,q"$prefix.raw")
    }
  }

  def genSetValue(prefix: c.Expr[c.PrefixType], tpe: Type, offset: Int, value: c.Tree): Tree =
    tpe match {
      case t if t =:= tCChar        => q"$prefix.raw.setByte($offset,$value)"
      case t if t =:= tCShort       => q"$prefix.raw.setShort($offset,$value)"
      case t if t =:= tCInt         => q"$prefix.raw.setInt($offset,$value)"
      case t if t =:= tCLong        => q"$prefix.raw.setNativeLong($offset,$value)"
      case t if t =:= tCLongLong    => q"$prefix.raw.setLong($offset,$value)"
      case t if t =:= tCUnsignedInt => q"$prefix.raw.setInt($offset,$value.toInt)"
      case t if t =:= tPtrByte      => q"$prefix.raw.setPointer($offset,$value.raw)"
    }

  def genFieldGetter(prefix: c.Expr[c.PrefixType], index: Int): Tree = {
    val typeArgs = prefix.actualType.dealias.typeArgs
    val offset = computeFieldOffset(typeArgs,index)
    genGetValue(prefix,typeArgs(index),offset)
  }

  def genFieldSetter(prefix: c.Expr[c.PrefixType], index: Int, value: Tree): Tree = {
    val typeArgs = prefix.actualType.dealias.typeArgs
    val offset = computeFieldOffset(typeArgs,index)
    genSetValue(prefix,typeArgs(index),offset,value)
  }

  def ptrToCStruct[T: c.WeakTypeTag](ptr: c.Tree): c.Tree = {
    val tpe = c.weakTypeOf[T].dealias
    tpe.typeArgs match {
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1) => q"new scalanative.unsafe.CStruct1[$t1]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2) => q"new scalanative.unsafe.CStruct2[$t1, $t2]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3) => q"new scalanative.unsafe.CStruct3[$t1, $t2, $t3]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4) => q"new scalanative.unsafe.CStruct4[$t1, $t2, $t3, $t4]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5) => q"new scalanative.unsafe.CStruct5[$t1, $t2, $t3, $t4, $t5]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6) => q"new scalanative.unsafe.CStruct6[$t1, $t2, $t3, $t4, $t5, $t6]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7) => q"new scalanative.unsafe.CStruct7[$t1, $t2, $t3, $t4, $t5, $t6, $t7]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8) => q"new scalanative.unsafe.CStruct8[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9) => q"new scalanative.unsafe.CStruct9[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) => q"new scalanative.unsafe.CStruct10[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) => q"new scalanative.unsafe.CStruct11[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12) => q"new scalanative.unsafe.CStruct12[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13) => q"new scalanative.unsafe.CStruct13[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14) => q"new scalanative.unsafe.CStruct14[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15) => q"new scalanative.unsafe.CStruct15[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16) => q"new scalanative.unsafe.CStruct16[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17) => q"new scalanative.unsafe.CStruct17[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18) => q"new scalanative.unsafe.CStruct18[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19) => q"new scalanative.unsafe.CStruct19[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20) => q"new scalanative.unsafe.CStruct20[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19, $t20]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 101)
      case List(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21) => q"new scalanative.unsafe.CStruct21[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8, $t9, $t10, $t11, $t12, $t13, $t14, $t15, $t16, $t17, $t18, $t19, $t20, $t21]($ptr.raw)"
// ###sourceLocation(file: "/Volumes/JKDATA/dev/scala-native/swog/interop/jvm/src/main/scala/scala/scalanative/unsafe/MacroTools.scala.gyb", line: 103)
    }
  }
}
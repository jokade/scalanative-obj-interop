import de.surfice.smacrotools.debug

import scala.scalanative.native.CObj.{CRef, Out}
import scalanative.native._
import CObj.implicits._

object Main {

  def main(args: Array[String]): Unit = Zone{ implicit z: Zone =>

  }

}

@CObj
class Foo

@CObj
@debug
class Bar extends Foo

//@CObj
//abstract class X

//@CObj
//class Y extends X

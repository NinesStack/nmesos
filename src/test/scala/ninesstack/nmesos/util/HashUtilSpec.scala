package ninesstack.nmesos.util

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class HashUtilSpec extends AnyFlatSpec with should.Matchers {

  "Hash" should "build a short hash" in {
    val hash = HashUtil.hash("test content")
    hash should be("1eebdf4")
  }

}

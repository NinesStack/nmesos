package ninesstack.nmesos.singularity

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class ModelConversionsSpec extends AnyFlatSpec with should.Matchers {

  "Singularity Model Conversion" should "Return a normalized compatible with Singularity" in {
    ModelConversions.normalizeId(
      "1.0.98_19f258e"
    ) should be("1_0_98_19f258e")
    ModelConversions.normalizeId("latest") should be("latest")
    ModelConversions.normalizeId(
      "2.1.43-Play258"
    ) should be("2_1_43_Play258")
  }

}

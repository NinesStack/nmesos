package com.nitro.nmesos.singularity

import org.specs2.mutable.Specification

class ModelConversionsSpec extends Specification {

  "Singularity Model Conversion" should {

    "Return a normalized compatible with Singularity" in {
      ModelConversions.normalizeId(
        "1.0.98_19f258e"
      ) must be equalTo "1_0_98_19f258e"
      ModelConversions.normalizeId("latest") must be equalTo "latest"
      ModelConversions.normalizeId(
        "2.1.43-Play258"
      ) must be equalTo "2_1_43_Play258"
    }

  }
}

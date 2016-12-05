package com.nitro.nmesos.util

import org.specs2.mutable.Specification

class HashUtilSpec extends Specification {

  "Hash" should {

    "build a short hash" in {
      val hash = HashUtil.hash("test content")
      hash must be equalTo "1eebdf4"
    }
  }
}

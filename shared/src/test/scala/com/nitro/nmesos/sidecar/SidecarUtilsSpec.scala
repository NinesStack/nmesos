package com.nitro.nmesos.sidecar

import org.specs2.mutable.Specification
import com.nitro.nmesos.util.{ Logger, CustomLogger }

class SidecarUtilsSpec extends Specification {

  implicit val log = CustomLogger(ansiEnabled = true, verbose = true)

  "Sidecar Utils" should {
    "diff the infos right" in {
      SidecarUtils.diffInfo(Seq("image @ host"), Seq("image @ host"), "service") should beTrue
      SidecarUtils.diffInfo(Seq(), Seq("image @ host"), "service") should beTrue
      SidecarUtils.diffInfo(Seq("image @ host"), Seq("image @ host", "image @ host2"), "service") should beFalse
      SidecarUtils.diffInfo(Seq("image @ host", "image @ host2)"), Seq("image @ host"), "service") should beTrue
    }
  }
}

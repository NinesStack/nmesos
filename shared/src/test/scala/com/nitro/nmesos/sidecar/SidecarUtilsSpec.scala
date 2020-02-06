package com.nitro.nmesos.sidecar

import org.specs2.mutable.Specification
import com.nitro.nmesos.util.{ Logger, CustomLogger }

class SidecarUtilsSpec extends Specification {

  implicit val log = CustomLogger(ansiEnabled = true, verbose = true)

  "Sidecar Utils" should {
    "diff the infos right" in {
      val containerNotDepoyedOnMesos = Seq()
      val goodContainerInfo, goodSidecarInfo = Seq("image @ host")
      val badContainerInfo, badSidecarInfo = Seq("image @ host", "image @ host2")

      SidecarUtils.diffInfo(goodContainerInfo, goodSidecarInfo, "service") should beTrue
      SidecarUtils.diffInfo(containerNotDepoyedOnMesos, goodSidecarInfo, "service") should beTrue
      SidecarUtils.diffInfo(goodContainerInfo, badSidecarInfo, "service") should beFalse
      SidecarUtils.diffInfo(badContainerInfo, goodSidecarInfo, "service") should beFalse
    }
  }
}

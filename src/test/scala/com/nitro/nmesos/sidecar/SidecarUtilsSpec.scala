package com.nitro.nmesos.sidecar

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import com.nitro.nmesos.util.{Formatter, CustomFormatter}

class SidecarUtilsSpec extends AnyFlatSpec with should.Matchers {

  implicit val fmt: CustomFormatter =
    CustomFormatter(ansiEnabled = true, verbose = true)

  "Sidecar Utils" should "diff the infos right" in {
    val containerNotDepoyedOnMesos = Seq()
    val goodContainerInfo, goodSidecarInfo = Seq("image @ host")
    val badContainerInfo, badSidecarInfo =
      Seq("image @ host", "image @ host2")

    SidecarUtils.diffInfo(
      goodContainerInfo,
      goodSidecarInfo,
      "service"
    ) should be(true)
    SidecarUtils.diffInfo(
      containerNotDepoyedOnMesos,
      goodSidecarInfo,
      "service"
    ) should be(true)
    SidecarUtils.diffInfo(
      goodContainerInfo,
      badSidecarInfo,
      "service"
    ) should be(false)
    SidecarUtils.diffInfo(
      badContainerInfo,
      goodSidecarInfo,
      "service"
    ) should be(false)
  }

}

package ninesstack.nmesos.sidecar

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import ninesstack.nmesos.util.{Formatter, CustomFormatter}

class SidecarUtilsSpec extends AnyFlatSpec with should.Matchers {

  implicit val fmt: CustomFormatter =
    CustomFormatter(ansiEnabled = true)

  "Sidecar Utils" should "diff the infos right" in {
    val containerInfo, sidecarInfo = Seq("image @ host", "image @ host2")
    val subsetContainerInfo, subsetSidecarInfo = containerInfo.take(1)
    val supersetContainerInfo, supersetSidecarInfo = containerInfo ++ List("image @ host3")

    SidecarUtils.diffInfo(
      containerInfo,
      sidecarInfo,
      "service"
    ) should be(true)

    // these 2 are the weird cases. It is ok to find containers in sidecar,
    // that are not deployed with/through nmesos
    SidecarUtils.diffInfo(
      subsetContainerInfo,
      sidecarInfo,
      "service"
    ) should be(true)

    SidecarUtils.diffInfo(
      containerInfo,
      supersetSidecarInfo,
      "service"
    ) should be(true)

    // also test for the ultimate subset :)
    SidecarUtils.diffInfo(
      Seq.empty,
      sidecarInfo,
      "service"
    ) should be(true)

    SidecarUtils.diffInfo(
      supersetContainerInfo,
      sidecarInfo,
      "service"
    ) should be(false)

    SidecarUtils.diffInfo(
      containerInfo,
      subsetSidecarInfo,
      "service"
    ) should be(false)

  }

}

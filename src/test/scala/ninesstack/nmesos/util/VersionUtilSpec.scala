package ninesstack.nmesos.util

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.util.Success

class VersionUtilSpec extends AnyFlatSpec with should.Matchers {

  "VersionUtil" should "extract an expected version" in {
    VersionUtil.tryExtract("0.2.1") should be(Success(List(0, 2, 1)))
    VersionUtil.tryExtract("1.0.1-SNAPSHOT") should be(Success(List(1, 0, 1)))
    VersionUtil.tryExtract(
      "nmesos_version: '1.2.3' ## Min nmesos required to execute this config"
    ) should be(Success(List(1, 2, 3)))
  }

  it should "extract the version of a yaml file" in {
    val Yaml =
      """## Config to deploy exampleServer to Mesos using Singularity
          |##
          |
          |nmesos_version: '2.0.3' ## Min nmesos required to execute this config
          |common: &common
          |  image: ninesstack/test
        """.stripMargin
    VersionUtil.tryExtractFromYaml(Yaml) should be(Success(List(2, 0, 3)))

  }

  it should "Verify compatible versions" in {
    VersionUtil.isCompatible(
      requiredVersion = List(0, 0, 1),
      installedVersion = List(0, 1, 0)
    ) should be(true)
    VersionUtil.isCompatible(
      requiredVersion = List(0, 0, 1),
      installedVersion = List(0, 0, 1)
    ) should be(true)
    VersionUtil.isCompatible(
      requiredVersion = List(0, 0, 2),
      installedVersion = List(0, 0, 1)
    ) should be(false)
    VersionUtil.isCompatible(
      requiredVersion = List(0, 1, 0),
      installedVersion = List(0, 1, 0)
    ) should be(true)
    VersionUtil.isCompatible(
      requiredVersion = List(1, 1, 0),
      installedVersion = List(1, 1, 10)
    ) should be(true)
  }

}

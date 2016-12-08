package com.nitro.nmesos.util

import org.specs2.mutable.Specification

import scala.util.Success

class VersionUtilSpec extends Specification {

  "VersionUtil" should {

    "extract an expected version" in {
      VersionUtil.tryExtract("0.2.1") must be equalTo Success(List(0, 2, 1))
      VersionUtil.tryExtract("1.0.1-SNAPSHOT") must be equalTo Success(List(1, 0, 1))
      VersionUtil.tryExtract("nmesos_version: '1.2.3' ## Min nmesos required to execute this config") must be equalTo Success(List(1, 2, 3))
    }

    "extract the version of a yaml file" in {
      val Yaml =
        """## Config to deploy $name$ to Mesos using Singularity
          |##
          |
          |nmesos_version: '2.0.3' ## Min nmesos required to execute this config
          |common: &common
          |  image: gonitro/test
        """.stripMargin
      VersionUtil.tryExtractFromYaml(Yaml) must be equalTo Success(List(2, 0, 3))

    }
  }
}

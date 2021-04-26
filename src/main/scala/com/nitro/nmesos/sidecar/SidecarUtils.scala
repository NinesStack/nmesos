package com.nitro.nmesos.sidecar

import com.nitro.nmesos.docker.model.Container
import com.nitro.nmesos.singularity.model.SingularityRequestParent
import com.nitro.nmesos.util.Formatter

case class EnvironmentInfo(
    requests: Seq[SingularityRequestParent],
    containers: Seq[Container],
    sidecarsServices: Seq[SidecarServices]
)

object SidecarUtils {

  /**
    * Verify all the Sidecar state are in-sync across all the servers running in the cluster.
    * @param info
    * @return
    */
  def verifyInSync(info: EnvironmentInfo)(implicit fmt: Formatter): Boolean = {
    info.sidecarsServices
      .sliding(2)
      .map {
        case Seq(sidecarA, sidecarB) =>
          val diff = sidecarA.Services.values.toSeq.flatten
            .sortBy(_.ID)
            .diff(sidecarB.Services.values.toSeq.flatten.sortBy(_.ID))
          if (diff.nonEmpty) {

            fmt.println(
              s""" ${fmt.Fail} Sidecar is not in sync at ${sidecarB.hostName} for services ${diff
                .map(_.Name)
                .mkString(",")}"""
            )
          } else {
            fmt.println(
              s""" ${fmt.Ok} Sidecar running at ${sidecarA.hostName} in sync with ${sidecarB.hostName} """
            )
          }
          diff.isEmpty
      }
      .reduce(_ && _)
  }

  /**
    * Verify the containers (integrated with Sidecar) and Sidecar state is in sync.
    */
  def verifyServices(info: EnvironmentInfo)(implicit fmt: Formatter): Boolean = {
    // fetch all Containers where SidecarDiscover!=false
    val containersByServiceName = info.containers
      .filter(!_.env.exists {
        case (key, value) => key == "SidecarDiscover" && value == "false"
      })
      .groupBy(
        _.env
          .find { case (key, _) => key == "ServiceName" }
          .map(_._2)
          .getOrElse("")
      ) // group by serviceName

    // Fetch Sidecar info about by Service
    val sidecarByServiceName = info.sidecarsServices.head.Services

    // Compare containers and service info
    sidecarByServiceName
      .map {
        case (serviceName, sidecarEntries) =>
          val containerInfo = containersByServiceName
            .get(serviceName)
            .getOrElse(Seq.empty)
            .map(c => s"${c.image} @ ${c.host}")
            .sorted

          val sidecarInfo = sidecarEntries
            .filter(_.Status == 0)
            .map { s => s"${s.Image} @ ${s.Hostname}" }
            .sorted

          diffInfo(containerInfo, sidecarInfo, serviceName)
      }
      .reduce(_ && _)
  }

  def diffInfo(
      containerInfo: Seq[String],
      sidecarInfo: Seq[String],
      serviceName: String
  )(implicit fmt: Formatter): Boolean = {
    val moreContainerInfos = containerInfo.diff(sidecarInfo) 
    val moreSidecarInfos = sidecarInfo.diff(containerInfo)

    if (moreContainerInfos.isEmpty && moreSidecarInfos.isEmpty) {
      fmt.println(
        s""" ${fmt.Ok} Sidecar mapping for ${serviceName} match all containers running """
      )
      containerInfo.foreach { info =>
        fmt.info(s"\t\t${info}")
      }
      true
    } else if (moreContainerInfos.isEmpty && !moreSidecarInfos.isEmpty) {
      fmt.println(
        s""" ${fmt.Ok} Sidecar mapping for ${serviceName} matches all containers running """
      )
      containerInfo.foreach { info =>
        fmt.info(s"\t\t${info}}")
      }
     
      fmt.warn("\tFound (in Sidecar, but not in Mesos):")
      moreSidecarInfos.foreach { info =>
        fmt.warn(s"\t\t${info}")
      }
      true
    } else {
      fmt.println(s""" ${fmt.Fail} Invalid Sidecar mapping for ${serviceName} """)
      fmt.error("\tExpected (Containers running):")
      containerInfo.foreach { info =>
        fmt.error(s"\t\t${info}")
      }
      fmt.error("\tFound (in Sidecar):")
      sidecarInfo.foreach { info =>
        fmt.error(s"\t\t${info}")
      }
      false
    }
  }
}

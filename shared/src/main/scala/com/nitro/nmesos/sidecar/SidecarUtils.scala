package com.nitro.nmesos.sidecar

import com.nitro.nmesos.docker.model.Container
import com.nitro.nmesos.singularity.model.SingularityRequestParent
import com.nitro.nmesos.util.Logger

case class EnvironmentInfo(requests: Seq[SingularityRequestParent], containers: Seq[Container], sidecarsServices: Seq[SidecarServices])

object SidecarUtils {

  /**
   * Verify all the Sidecar state are in-sync across all the servers running in the cluster.
   * @param info
   * @return
   */
  def verifyInSync(info: EnvironmentInfo)(implicit log: Logger): Boolean = {
    info.sidecarsServices.sliding(2).map {
      case Seq(sidecarA, sidecarB) =>
        val diff = sidecarA.Services.values.toSeq.flatten.sortBy(_.ID).diff(sidecarB.Services.values.toSeq.flatten.sortBy(_.ID))
        if (diff.nonEmpty) {

          log.println(s""" ${log.Fail} Sidecar is not in sync at ${sidecarB.hostName} for services ${diff.map(_.Name).mkString(",")}""")
        } else {
          log.println(s""" ${log.Ok} Sidecar running at ${sidecarA.hostName} in sync with ${sidecarB.hostName} """)
        }
        diff.isEmpty
    }.reduce(_ && _)
  }

  /**
   * Verify the containers (integrated with Sidecar) and Sidecar state is in sync.
   */
  def verifyServices(info: EnvironmentInfo)(implicit log: Logger): Boolean = {
    // fetch all Containers where SidecarDiscover!=false
    val containersByServiceName = info.containers
      .filter(!_.env.exists { case (key, value) => key == "SidecarDiscover" && value == "false" })
      .groupBy(_.env.find { case (key, _) => key == "ServiceName" }.map(_._2).getOrElse("")) // group by serviceName

    // Fetch Sidecar info about by Service
    val sidecarByServiceName = info.sidecarsServices.head.Services

    // Compare containers and service info
    sidecarByServiceName.map {
      case (serviceName, sidecarEntries) =>
        val containerInfo = containersByServiceName.get(serviceName).getOrElse(Seq.empty)
          .map(c => s"${c.image} @ ${c.host}").sorted

        val sidecarInfo = sidecarEntries.filter(_.Status == 0)
          .map { s => s"${s.Image} @ ${s.Hostname}" }.sorted

        diffInfo(containerInfo, sidecarInfo, serviceName)
    }.reduce(_ && _)
  }

  def diffInfo(containerInfo: Seq[String], sidecarInfo: Seq[String], serviceName: String)(implicit log: Logger): Boolean = {
    val diff = sidecarInfo.diff(containerInfo)
    if (diff.isEmpty) {
      log.println(s""" ${log.Ok} Sidecar mapping for $serviceName match all containers running """)
      sidecarInfo.foreach { info =>
        log.info(s"\t\t$info")
      }
      true
    } else if (containerInfo.isEmpty) {
      log.info("\tFound (in Sidecar, but not in mesos):")
      sidecarInfo.foreach { info =>
        log.info(s"\t\t$info")
      }
      true
    } else {
      log.println(s""" ${log.Fail} Invalid Sidecar mapping for $serviceName""")
      log.error("\tExpected (Containers running):")
      containerInfo.foreach { info =>
        log.error(s"\t\t$info")
      }
      log.error("\tFound (in Sidecar):")
      sidecarInfo.foreach { info =>
        log.error(s"\t\t$info")
      }
      false
    }
  }
}

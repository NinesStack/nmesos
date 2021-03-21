package com.nitro.nmesos.singularity

import com.nitro.nmesos.singularity.ModelConversions.DeployId
import com.nitro.nmesos.singularity.model.SingularityRequestParent.SingularityActiveDeployResponse
import com.nitro.nmesos.singularity.model.SingularityTask.SingularityTaskInfo
import com.nitro.nmesos.util.CustomPicklers.OptionPickler.{ReadWriter => RW, macroRW}

/**
  * Subset of the Singularity api Model.
  *
  * @see http://getsingularity.com/Docs/reference/apidocs/models.html
  */
object model {

  // Mesos resources
  case class SingularityResources(
      cpus: Double,
      memoryMb: Double,
      numPorts: Int,
      diskMb: Int
  )

  object SingularityResources {
    implicit val rw: RW[SingularityResources] = macroRW
  }

  case class SingularityDockerPortMapping(
      containerPort: Int,
      hostPortType: String,
      containerPortType: String,
      hostPort: Int,
      protocol: String
  )

  object SingularityDockerPortMapping {
    implicit val rw:RW[SingularityDockerPortMapping] = macroRW
  }

  case class SingularityDockerInfo(
      network: String,
      image: String,
      portMappings: Seq[SingularityDockerPortMapping],
      forcePullImage: Boolean,
      dockerParameters: Seq[SingularityDockerParameter]
  )

  object SingularityDockerInfo {
    implicit val rw:RW[SingularityDockerInfo] = macroRW
  }

  case class SingularityDockerParameter(key: String, value: String)

  object SingularityDockerParameter {
    implicit val rw:RW[SingularityDockerParameter] = macroRW
  }

  case class SingularityVolume(
      hostPath: String,
      containerPath: String,
      mode: String
  )

  object SingularityVolume {
    implicit val rw:RW[SingularityVolume] = macroRW
  }

  case class SingularityContainerInfo(
      `type`: String,
      docker: SingularityDockerInfo,
      volumes: Seq[SingularityVolume]
  )

  object SingularityContainerInfo {
    implicit val rw:RW[SingularityContainerInfo] = macroRW
  }

  case class SingularityDeploy(
      requestId: String,
      id: String,
      resources: SingularityResources,
      containerInfo: SingularityContainerInfo,
      command: Option[String] = None,
      shell: Option[Boolean] = None,
      deployInstanceCountPerStep: Option[Int] = None,
      deployStepWaitTimeMs: Option[Int] = None,
      deployHealthTimeoutSeconds: Option[Int] = None,
      autoAdvanceDeploySteps: Option[Boolean] = None,
      customExecutorCmd: Option[String] = None,
      env: Map[String, String] = Map.empty,
      healthcheckUri: Option[String] = None,
      healthcheckPortIndex: Option[Int] = None,
      healthcheckMaxRetries: Option[Int] = None,
      healthcheckTimeoutSeconds: Option[Int] = None,
      healthcheckMaxTotalTimeoutSeconds: Option[Int] = None
  )

  object SingularityDeploy {
    implicit val rw:RW[SingularityDeploy] = macroRW
  }

  case class SingularityRequest(
      id: String,
      requestType: String,
      slavePlacement: String,
      instances: Option[Int] = None,
      schedule: Option[String] = None,
      requiredSlaveAttributes: Map[String, String] = Map.empty,
      allowedSlaveAttributes: Map[String, String] = Map.empty,
      requiredRole: Option[String] = None
  )

  object SingularityRequest {
    implicit val rw:RW[SingularityRequest] = macroRW
  }

  case class SingularityDeployRequest(
      deploy: SingularityDeploy,
      message: String,
      unpauseOnSuccessfulDeploy: Boolean
  )

  object SingularityDeployRequest {
    implicit val rw:RW[SingularityDeployRequest] = macroRW
  }

  case class SingularityScaleRequest(
      message: String,
      instances: Option[Int] = None,
      skipHealthchecks: Boolean =
        true //If set to true, healthchecks will be skipped while scaling this request (only)
  )

  object SingularityScaleRequest {
    implicit val rw:RW[SingularityScaleRequest] = macroRW
  }

  object SingularityRequestParent {
    case class SingularityActiveDeployResponse(
        id: DeployId,
        resources: SingularityResources
    )

    object SingularityActiveDeployResponse {
      implicit val rw:RW[SingularityActiveDeployResponse] = macroRW
    }

    implicit val rw:RW[SingularityRequestParent] = macroRW
  }

  case class SingularityRequestParent(
      request: SingularityRequest,
      state: String,
      taskIds: Option[SingularityTaskIdsByStatus] = None,
      activeDeploy: Option[SingularityActiveDeployResponse] = None
  )

  case class SingularityDeployResult(
      deployState: String,
      message: Option[String] = None,
      deployFailures: Seq[SingularityDeployFailure] = Seq.empty
  )

  object SingularityDeployResult {
    implicit val rw:RW[SingularityDeployResult] = macroRW
  }

  case class SingularityDeployFailure(
      reason: String,
      taskId: SingularityTaskId,
      message: Option[String] = None
  )

  object SingularityDeployFailure {
    implicit val rw:RW[SingularityDeployFailure] = macroRW
  }

  case class SingularityUpdateResult(state: String)

  object SingularityUpdateResult {
    implicit val rw:RW[SingularityUpdateResult] = macroRW
  }

  case class SingularityDeployHistory(
      deploy: SingularityDeploy,
      deployResult: Option[SingularityDeployResult] = None
  )

  object SingularityDeployHistory {
    implicit val rw:RW[SingularityDeployHistory] = macroRW
  }

  case class SingularityPendingDeploy(
      currentDeployState: String,
      deployMarker: SingularityDeployMarker,
      deployProgress: SingularityDeployProgress
  )

  object SingularityPendingDeploy {
    implicit val rw:RW[SingularityPendingDeploy] = macroRW
  }

  case class SingularityDeployProgress(targetActiveInstances: Int)

  object SingularityDeployProgress {
    implicit val rw:RW[SingularityDeployProgress] = macroRW
  }

  case class SingularityDeployMarker(requestId: String, deployId: String)

  object SingularityDeployMarker {
    implicit val rw:RW[SingularityDeployMarker] = macroRW
  }

  case class SingularityTaskId(
      requestId: String,
      host: String,
      sanitizedHost: String,
      deployId: String,
      id: String,
      instanceNo: Int
  )

  object SingularityTaskId {
    implicit val rw:RW[SingularityTaskId] = macroRW
  }

  case class SingularityTaskIdHistory(
      lastTaskState: String,
      taskId: SingularityTaskId
  )

  object SingularityTaskIdHistory {
    implicit val rw:RW[SingularityTaskIdHistory] = macroRW
  }

  object SingularityTask {

    case class SingularityTasKDockerPortMapping(
        containerPort: Int,
        hostPort: Int
    )

    object SingularityTasKDockerPortMapping {
      implicit val rw:RW[SingularityTasKDockerPortMapping] = macroRW
    }

    case class SingularityTaskDockerInfo(
        portMappings: Seq[SingularityTasKDockerPortMapping]
    )

    object SingularityTaskDockerInfo {
      implicit val rw:RW[SingularityTaskDockerInfo] = macroRW
    }

    case class SingularityTaskIContainerInfo(docker: SingularityTaskDockerInfo)

    object SingularityTaskIContainerInfo {
      implicit val rw:RW[SingularityTaskIContainerInfo] = macroRW
    }

    case class SingularityTaskInfo(container: SingularityTaskIContainerInfo)

    object SingularityTaskInfo {
      implicit val rw:RW[SingularityTaskInfo] = macroRW
    }

    implicit val rw:RW[SingularityTask] = macroRW
  }

  case class SingularityTask(
      taskId: SingularityTaskId,
      mesosTask: SingularityTaskInfo,
      offer: Offer
  )

  case class Offer(hostname: String)

  object Offer {
    implicit val rw:RW[Offer] = macroRW
  }

  case class SingularityLog(data: String, offset: Int)

  object SingularityLog {
    implicit val rw:RW[SingularityLog] = macroRW
  }

  case class SingularityTaskIdsByStatus(healthy: Seq[SingularityTaskId])

  object SingularityTaskIdsByStatus {
    implicit val rw:RW[SingularityTaskIdsByStatus] = macroRW
  }
}

package ninesstack.nmesos.config

/**
  * Yaml file model.
  * ConfigReader.parse(file) load a file into this model.
  */
object model {

  type EnvironmentName = String

  case class CmdConfig(
      serviceName: String,
      tag: String,
      force: Boolean,
      environmentName: EnvironmentName,
      environment: Environment,
      fileHash: String,
      file: java.io.File
  )

  case class Config(
      nmesos_version: String,
      environments: Map[EnvironmentName, Environment]
  )

  case class Environment(
      resources: Resources,
      container: Container,
      executor: Option[ExecutorConf],
      singularity: SingularityConf,
      after_deploy: Option[AfterDeployConf]
  )

  case class Resources(cpus: Double, memoryMb: Int, instances: Option[Int])

  case class PortMap(
      containerPort: Int,
      hostPort: Option[Int],
      protocols: Option[String]
  )

  case class Container(
      image: String,
      command: Option[String],
      forcePullImage: Option[Boolean],
      ports: Option[Seq[PortMap]],
      labels: Option[Map[String, String]],
      env_vars: Option[Map[String, String]],
      volumes: Option[Seq[String]],
      network: Option[String],
      dockerParameters: Option[Map[String, String]],
      deploy_freeze: Option[Boolean]
  )

  case class SingularityConf(
      url: String,
      schedule: Option[String],
      requestType: Option[String],
      deployInstanceCountPerStep: Option[Int],
      deployStepWaitTimeMs: Option[Int],
      deployHealthTimeoutSeconds: Option[Int],
      autoAdvanceDeploySteps: Option[Boolean],
      healthcheckUri: Option[String],
      healthcheckPortIndex: Option[Int],
      healthcheckMaxRetries: Option[Int],
      healthcheckTimeoutSeconds: Option[Int],
      healthcheckMaxTotalTimeoutSeconds: Option[Int],
      requiredRole: Option[String],
      requiredAttributes: Option[Map[String, String]],
      allowedSlaveAttributes: Option[Map[String, String]],
      slavePlacement: Option[String]
  )

  case class ExecutorConf(
      customExecutorCmd: Option[String],
      env_vars: Option[Map[String, String]]
  )

  case class AfterDeployConf(
      on_success: List[DeployJob],
      on_failure: Option[DeployJob]
  )

  case class DeployJob(service_name: String, tag: Option[String])
}

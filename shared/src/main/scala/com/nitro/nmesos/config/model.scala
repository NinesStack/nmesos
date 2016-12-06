package com.nitro.nmesos.config

import java.io.File

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
    file: File
  )

  case class Config(
    nmesos_version: String,
    environments: Map[EnvironmentName, Environment]
  )

  case class Environment(
    resources: Resources,
    container: Container,
    mesos: MesosConf,
    executor: Option[ExecutorConf],
    singularity: SingularityConf
  )

  case class Resources(
    instances: Int,
    cpus: Double,
    memoryMb: Int
  )

  case class Container(
    image: String,
    ports: Option[Seq[Int]],
    labels: Option[Map[String, String]],
    env_vars: Option[Map[String, String]],
    volumes: Option[Seq[String]],
    network: Option[String],
    dockerParameters: Option[Map[String, String]]
  )

  case class MesosConf(
    url: String
  )

  case class SingularityConf(
    url: String,
    deployInstanceCountPerStep: Int,
    deployStepWaitTimeMs: Option[Int],
    autoAdvanceDeploySteps: Boolean,
    healthcheckUri: Option[String],
    slavePlacement: Option[String]
  )

  case class ExecutorConf(
    customExecutorCmd: Option[String],
    env_vars: Map[String, String]
  )
}
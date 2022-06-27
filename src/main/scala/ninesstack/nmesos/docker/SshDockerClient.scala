package ninesstack.nmesos.docker

import ninesstack.nmesos.docker.model.Container

import scala.language.postfixOps

import sys.process._

/**
  * Run Remote Docker commands through ssh to inspect the containers running.
  */
object SshDockerClient {
  private val logger = org.apache.log4j.Logger.getLogger(this.getClass.getName)

  /**
    * Docker ps and inspect all containers in the host.
    */
  def fetchContainers(host: String): Seq[Container] = {
    val delimiter = "~"
    val containers = parseOutput(dockerPs(host, delimiter), host, delimiter)
    fetchContainersEnv(host, containers, delimiter)
  }

  private def dockerPs(host: String, delimiter: String) = {
    val format = s""" '{{.ID}}${delimiter}{{.Image}}${delimiter}{{.Names}}' """
    val command = s""" ssh -oStrictHostKeyChecking=no ${host} "docker ps --format ${format}" """
    logger.info(s"command: ${command}")
    command !!
  }

  private def parseOutput(psOutput: String, host: String, delimiter: String): Seq[Container] = {
    val result = psOutput.trim
      .split("\n")
      .map(_.split(delimiter).toList)
      .map {
        case (id :: image :: name :: Nil) =>
          Container(id, image, name, host, Map.empty)
        case _ =>
          sys.error(s"Unable to parse Docker output: ${psOutput}")
      }
      .toSeq
    logger.info(s"result: ${result}")
    result
  }

  /**
    * For each container fetch the environment vars
    */
  private def fetchContainersEnv(
    host: String,
    containers: Seq[Container],
    delimiter: String
  ): Seq[Container] = {
    val output = dockerInspectEnv(host, containers.map(_.id), delimiter)
    val lines = output.trim.split(s"\n|${delimiter}")

    val linesByContainer = lines
      .filterNot(_.isEmpty)
      .groupBy(_.takeWhile(_ != ':').take(12)) // Group by short Container Id
      .view.mapValues(_.map(_.dropWhile(_ != ':').drop(1))) // raw env vars as value

    val envByContainer = linesByContainer.mapValues { lines =>
      lines
        .map(_.split("=", 2) match { case Array(key, value) => (key, value) })
        .toMap
    }

    val result = containers.map { c =>
      c.copy(env = envByContainer.get(c.id).getOrElse(Map.empty))
    }
    logger.info(s"result: ${result}")
    result
  }

  private def dockerInspectEnv(host: String, containersId: Seq[String], delimiter: String) = {
    val cids = containersId.mkString(" ")
    val format = s""" '{{range .Config.Env}}{{$$.ID}}:{{.}}${delimiter}{{end}}${delimiter}{{range $$key, $$value := .Config.Labels}}{{$$.ID}}:{{$$key}}={{$$value}}${delimiter}{{end}}' """
    val command = s""" ssh -oStrictHostKeyChecking=no ${host} "docker inspect ${cids} --format ${format}" """
    logger.info(s"command: ${command}")
    command !!
  }

  private def parseTaskId(inspectOutput: String) = {
    inspectOutput
      .split(" ")
      .find(_.startsWith("TASK_ID="))
      .map(_.dropWhile(_ != '=').drop(1))
  }
}

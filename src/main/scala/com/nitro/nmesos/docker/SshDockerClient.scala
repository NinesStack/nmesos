package com.nitro.nmesos.docker

import com.nitro.nmesos.docker.model.Container

import scala.language.postfixOps

import sys.process._

/**
  * Run Remote Docker commands through ssh to inspect the containers running.
  */
object SshDockerClient {

  /**
    * Docker ps and inspect all containers in the host.
    */
  def fetchContainers(host: String): Seq[Container] = {
    val containers = parseOutput(dockerPs(host), host)
    fetchContainersEnv(host, containers)
  }

  // Docker ps
  private def dockerPs(host: String) = {
    s"""ssh -oStrictHostKeyChecking=no $host  docker ps --format "{{.ID}}\\t{{.Image}}\\t{{.Names}}" """ !!
  }

  // Docker inspect
  // Fetch labels and environments var
  private def dockerInspectEnv(host: String, containersId: Seq[String]) = {
    s"""ssh -oStrictHostKeyChecking=no $host docker inspect ${containersId
      .mkString(
        " "
      )} --format '{{range .Config.Env }}{{ $$.ID}}:{{.}}~{{end}}~{{range  $$key, $$value := .Config.Labels}}{{ $$.ID}}:{{$$key}}={{$$value}}~{{end}}' """ !!
  }

  /**
    * For each container fetch the environment vars
    */
  private def fetchContainersEnv(
      host: String,
      containers: Seq[Container]
  ): Seq[Container] = {

    val output = dockerInspectEnv(host, containers.map(_.id))

    val lines = output.trim
      .split("\n|~")

    val linesByContainer = lines
      .filterNot(_.isEmpty)
      .groupBy(_.takeWhile(_ != ':').take(12)) // Group by short Container Id
      .view.mapValues(_.map(_.dropWhile(_ != ':').drop(1))) // raw env vars as value

    val envByContainer = linesByContainer.mapValues { lines =>
      lines
        .map(_.split("=", 2) match { case Array(key, value) => (key, value) })
        .toMap
    }

    containers.map { c =>
      c.copy(env = envByContainer.get(c.id).getOrElse(Map.empty))
    }

  }

  private def parseTaskId(inspectOutput: String) = {
    inspectOutput
      .split(" ")
      .find(_.startsWith("TASK_ID="))
      .map(_.dropWhile(_ != '=').drop(1))
  }

  private def parseOutput(psOutput: String, host: String): Seq[Container] = {
    psOutput.trim
      .split("\n")
      .map(_.split("\t").toList)
      .map {
        case (id :: image :: name :: Nil) =>
          Container(id, image, name, host, Map.empty)
        case _ => sys.error(s"""Unable to parse Docker output:
             |$psOutput
          """.stripMargin)
      }
    .toSeq
  }
}

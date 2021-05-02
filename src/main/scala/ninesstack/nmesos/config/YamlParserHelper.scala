package ninesstack.nmesos.config

import ninesstack.nmesos.config.model._
import net.jcazevedo.moultingyaml._

import scala.annotation.tailrec

// Yaml parser Boilerplate
object YamlParserHelper {
  def parsePortMap(portMap: String, protocols: Option[String]): PortMap = {
    val (containerPort, hostPort) =
      portMap.split(":").map(_.toInt).toList match {
        case containerPort :: Nil             => (containerPort, None)
        case containerPort :: hostPort :: Nil => (containerPort, Some(hostPort))
        case _ =>
          deserializationError(
            "Failed to deserialize the port map specification"
          )
      }

    PortMap(containerPort, hostPort, protocols)
  }

  // boilerplate to parse our custom case class
  object YamlCustomProtocol extends DefaultYamlProtocol {
    implicit val PortMapYamlFormat: YamlFormat[PortMap] = new YamlFormat[PortMap] {
      override def read(yaml: YamlValue): PortMap =
        yaml match {
          case YamlNumber(_) => PortMap(yaml.convertTo[Int], None, None)
          case YamlString(_) =>
            yaml.convertTo[String].split("/").toList match {
              case portMap :: Nil => parsePortMap(portMap, None)
              case portMap :: protocols :: Nil =>
                parsePortMap(portMap, Option(protocols))
              case _ =>
                deserializationError(
                  "Failed to deserialize the port map specification"
                )
            }
          case _ =>
            deserializationError("Failed to deserialize the port specification")
        }

      override def write(portMap: PortMap): YamlValue = {
        portMap.protocols match {
          case None =>
            portMap.hostPort match {
              case Some(hostPort) =>
                YamlString(s"${portMap.containerPort}:${hostPort}")
              case None => YamlNumber(portMap.containerPort)
            }
          case Some(protocols) =>
            portMap.hostPort match {
              case Some(hostPort) =>
                YamlString(s"${portMap.containerPort}:${hostPort}/${protocols}")
              case None => YamlString(s"${portMap.containerPort}/${protocols}")
            }
        }
      }
    }

    implicit val resourcesFormat: YamlFormat[Resources] = yamlFormat3(Resources.apply)
    implicit val containerFormat: YamlFormat[Container] = yamlFormat10(Container.apply)
    implicit val singularityFormat: YamlFormat[SingularityConf] = yamlFormat16(SingularityConf.apply)
    implicit val executorFormat: YamlFormat[ExecutorConf] = yamlFormat2(ExecutorConf.apply)
    implicit val deployJobFormat: YamlFormat[DeployJob] = yamlFormat2(DeployJob.apply)
    implicit val afterDeployFormat: YamlFormat[AfterDeployConf] = yamlFormat2(AfterDeployConf.apply)
    implicit val environmentFormat: YamlFormat[Environment] = yamlFormat5(Environment.apply)
    implicit val configFormat: YamlFormat[Config] = yamlFormat2(Config.apply)
  }

  /**
    * Custom merge from Commons into Environments.
    * The Standard Yaml Merge is only a hash one level merge, implementing a smarter deep merge here.
    * -> input example:
    * common:
    *   version: 1
    *   resources:
    *   cpus: 1
    *
    * environments:
    *   dev:
    *   resources:
    *   mem: 1024
    *
    * -> out example:
    * environments:
    *   dev:
    *     version: 1
    *     resources:
    *     cpus: 1
    *     mem: 1024
    */
  def mergeCommonsIntoEnvironments(yaml: YamlObject): YamlObject = {
    val commonKey = YamlString("common")
    val common = yaml.fields(commonKey).asYamlObject
    val environments: Map[YamlValue, YamlValue] =
      yaml.fields(YamlString("environments")).asYamlObject.fields

    // Add environments fields recursively
    val updatedEnvironments = environments.view.mapValues {
      environment => smartMerge(common.fields, environment.asYamlObject)
    }

    // return new Yaml with commons and environments joined
    val updatedFields =
      yaml.fields.view.filterKeys(_ != commonKey) ++
        Map(YamlString("environments") -> new YamlObject(updatedEnvironments.toMap))
    new YamlObject(updatedFields.toMap)
  }

  private def smartMerge(
      initialFields: Map[YamlValue, YamlValue],
      element: YamlObject
  ): YamlObject = {
    val fields = element.fields.foldLeft(initialFields) {
      case (mergedFields, (key, value)) =>
        if (!mergedFields.contains(key) || mergedFields(key) == YamlNull) {
          mergedFields + (key -> value)
        } else {
          // already exist, deep merge the objects.
          value match {
            case yamlObject: YamlObject =>
              val previousObjectField = mergedFields(key).asYamlObject.fields
              mergedFields + (key -> smartMerge(
                previousObjectField,
                yamlObject
              ))

            case YamlNull | YamlBoolean(_) | YamlDate(_) | YamlNumber(_) |
                YamlString(_) =>
              mergedFields + (key -> value)

            case YamlArray(values) =>
              val previousArray =
                mergedFields(key).asInstanceOf[YamlArray].elements
              val distinctValues = Set((values ++ previousArray): _*)
              mergedFields + (key -> YamlArray(distinctValues.toVector))

            case YamlSet(values) =>
              val previousSet = mergedFields(key).asInstanceOf[YamlSet].set
              mergedFields + (key -> YamlSet(previousSet ++ values))

            case _ => throw new RuntimeException("Unexpected case")
          }
        }
    }
    new YamlObject(fields)
  }
}

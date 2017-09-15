package com.nitro.nmesos.config

import com.nitro.nmesos.config.model._
import net.jcazevedo.moultingyaml.{ DefaultYamlProtocol, YamlArray, YamlBoolean, YamlDate, YamlFormat, YamlNull, YamlNumber, YamlObject, YamlSet, YamlString, YamlValue, deserializationError }

import scala.annotation.tailrec

// Yaml parser Boilerplate
object YamlParserHelper {

  // boilerplate to parse our custom case class
  object YamlCustomProtocol extends DefaultYamlProtocol {
    implicit val PortMapYamlFormat = new YamlFormat[PortMap] {
      override def read(yaml: YamlValue): PortMap = yaml match {
        case YamlNumber(_) => PortMap(yaml.convertTo[Int], None)
        case YamlString(_) => yaml.convertTo[String].split(":").map(_.toInt).toList match {
          case containerPort :: Nil => PortMap(containerPort, None)
          case containerPort :: hostPort :: Nil => PortMap(containerPort, Some(hostPort))
          case _ => deserializationError("Failed to deserialize port map specification")
        }
        case _ => deserializationError("Failed to deserialize port specification")

      }

      override def write(obj: PortMap): YamlValue = {
        YamlString(s"${obj.containerPort}:${obj.hostPort}")
      }
    }

    implicit val resourcesFormat = yamlFormat3(Resources)
    implicit val containerFormat = yamlFormat9(Container)
    implicit val singularityFormat = yamlFormat9(SingularityConf)
    implicit val executorFormat = yamlFormat2(ExecutorConf)
    implicit val environmentFormat = yamlFormat4(Environment)
    implicit val configFormat = yamlFormat2(Config)
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
   *
   */
  def mergeCommonsIntoEnvironments(yaml: YamlObject): YamlObject = {
    val commonKey = YamlString("common")
    val common = yaml.fields(commonKey).asYamlObject
    val environments: Map[YamlValue, YamlValue] = yaml.fields(YamlString("environments")).asYamlObject.fields

    var mergedFields = Map.empty[YamlValue, YamlValue]

    // Add commons fields
    mergedFields ++= common.fields

    // Add environments fields recursively
    val updatedEnvironments = environments.mapValues { environment =>
      smartMerge(common.fields, environment.asYamlObject)
    }

    // return new Yaml with commons and environments joined
    val updatedFields = yaml.fields.filterKeys(_ != commonKey) + (YamlString("environments") -> new YamlObject(updatedEnvironments))
    new YamlObject(updatedFields)
  }

  private def smartMerge(initialFields: Map[YamlValue, YamlValue], element: YamlObject): YamlObject = {
    val fields = element.fields.foldLeft(initialFields) {
      case (mergedFields, (key, value)) =>
        if (!mergedFields.contains(key)) {
          mergedFields + (key -> value)
        } else {
          // already exist, deep merge the objects.
          value match {
            case yamlObject: YamlObject =>
              val previousObjectField = mergedFields(key).asYamlObject.fields
              mergedFields + (key -> smartMerge(previousObjectField, yamlObject))

            case YamlNull | YamlBoolean(_) | YamlDate(_) | YamlNumber(_) | YamlString(_) =>
              mergedFields + (key -> value)

            case YamlArray(values) =>
              val previousArray = mergedFields(key).asInstanceOf[YamlArray].elements
              val distinctValues = Set((values ++ previousArray): _*)
              mergedFields + (key -> YamlArray(distinctValues.toVector))

            case YamlSet(values) =>
              val previousSet = mergedFields(key).asInstanceOf[YamlSet].set
              mergedFields + (key -> YamlSet(previousSet ++ values))
          }
        }
    }
    new YamlObject(fields)
  }
}

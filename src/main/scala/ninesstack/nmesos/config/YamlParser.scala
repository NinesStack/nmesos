package ninesstack.nmesos.config

import ninesstack.nmesos.config.YamlParserHelper._
import ninesstack.nmesos.config.model._
import ninesstack.nmesos.util.{HashUtil, Formatter}
import net.jcazevedo.moultingyaml._
import org.yaml.snakeyaml.parser.ParserException

import scala.util.{Failure, Success, Try}

/**
  * Yaml conf file to models
  */
object YamlParser {
  private val logger = org.log4s.getLogger

  sealed trait ParserResult

  case class InvalidYaml(msg: String) extends ParserResult

  case class ValidYaml(config: Config, hash: String) extends ParserResult

  /**
    * Try to parse a yaml.
    */
  def parse(sourceContent: String, fmt: Formatter): ParserResult =
    tryParse(sourceContent, fmt) match {

      case Success(config) =>
        ValidYaml(config, HashUtil.hash(sourceContent))

      case Failure(ex: DeserializationException) =>
        // Yaml data doesn't match our Case class, custom error message!
        val field = ex.fieldNames.mkString("/")
        InvalidYaml(s"Parser error for field $field: ${ex.msg}")

      case Failure(ex: ParserException) =>
        // Invalid Yaml here?
        val line = ex.getProblemMark.getLine
        val column = ex.getProblemMark.getColumn
        val snippet = ex.getProblemMark.get_snippet
        InvalidYaml(
          s"Invalid yaml file at line $line, column: $column\n$snippet"
        )

      case Failure(ex) =>
        logger.info(ex.getStackTrace.mkString("\n"))
        InvalidYaml(s"Unexpected error: ${ex.getMessage}")

    }

  // Parse and merge default into environments.
  private def tryParse(source: String, fmt: Formatter) =
    Try {
      import YamlCustomProtocol._
      val yaml = source.parseYaml

      // Custom merge to extend Yaml merge
      val smartYaml = mergeCommonsIntoEnvironments(yaml.asYamlObject)
      logger.info(s"Yaml evaluated content:\n ${smartYaml.prettyPrint}")

      smartYaml.convertTo[Config]
    }

}

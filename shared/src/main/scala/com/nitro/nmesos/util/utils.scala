package com.nitro.nmesos.util

import com.nitro.nmesos.BuildInfo
import org.joda.time.DateTime

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }

// Scala conversions boilerplate .
object Conversions {

  implicit class OptionToTry[A](opt: Option[A]) {
    def toTry(error: String) = opt match {
      case None => Failure(sys.error(error))
      case Some(value) => Success(value)
    }
  }

}

object HashUtil {

  val HashLength = 7

  // Small hash to detect file changes.
  def hash(content: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(content.getBytes("UTF-8")).map("%02x".format(_)).mkString.take(HashLength)
  }

}

object VersionUtil {

  import Conversions._

  val VersionPattern = """\D*(\d*)\.(\d*)\.(\d*).*""".r
  type Version = List[Int]
  val VersionField = "nmesos_version"

  def tryExtractFromYaml(sourceYml: String): Try[Version] = {
    val tryVersionLine = sourceYml.lines.find(_.contains(VersionField)).toTry(s"$VersionField no present in the config file")

    for {
      versionLine <- tryVersionLine
      version <- tryExtract(versionLine)
    } yield version
  }

  def tryExtract(version: String): Try[Version] = version match {
    case VersionPattern(major, minor, patch) =>
      Success(List(major.toInt, minor.toInt, patch.toInt))
    case invalid =>
      Failure(sys.error(s"Unable to extract version from $invalid"))
  }

  // Compare yaml required version vs installed version.
  def isCompatible(requiredVersion: Version, log: Logger): Boolean = {
    val resultTry = for {
      installed <- tryExtract(BuildInfo.version)
    } yield isCompatible(requiredVersion, installed)

    resultTry match {
      case Failure(ex) =>
        log.error(ex.getMessage)
        false
      case Success(result) => result
    }
  }

  // compare compatibility between semantic versions
  private def isCompatible(required: Version, installed: Version): Boolean = {
    required.zip(installed).foldLeft(true) {
      case (stillCompatible, (required, installed)) =>
        stillCompatible && required <= installed
    }
  }

}

object SequenceUtil {
  val SequenceBaseTime = new DateTime(2016, 12, 1, 0, 0)

  def sequenceId(): Long = (DateTime.now.getMillis / 1000) - (SequenceBaseTime.getMillis / 1000)
}

object WaitUtil {

  val DefaultWait = 1000
  // Wait for condition or failure.
  @tailrec
  def waitUntil(body: => Try[Boolean]): Unit = {
    body match {
      case Success(isValid) =>
        if (!isValid) {
          Thread.sleep(DefaultWait)
          waitUntil(body)
        }
      case Failure(_) => // Continue
    }
  }

}
package com.nitro.nmesos.util

import scala.util.{ Failure, Success }

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

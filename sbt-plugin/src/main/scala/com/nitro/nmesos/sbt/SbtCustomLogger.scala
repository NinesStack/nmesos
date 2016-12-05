package com.nitro.nmesos.sbt

import com.nitro.nmesos.util.Logger

/**
 * Redirect logs to the standard sbt logger.
 */
case class SbtCustomLogger(sbtLogger: sbt.Logger) extends Logger {
  override def error(msg: => Any): Unit = sbtLogger.error(errorColor(msg).toString)
  override def info(msg: => Any): Unit = sbtLogger.info(infoColor(msg).toString)
  override def println(msg: => Any): Unit = sbtLogger.info(msg.toString)

  override def debug(msg: => Any): Unit = sbtLogger.debug(msg.toString)

  override def ansiEnabled: Boolean = true
}

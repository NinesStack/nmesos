package com.nitro.nmesos.util

import scala.annotation.tailrec

/**
  * Format basic terminal output for the CLI app.
  */
trait Formatter extends AnsiFormatter with Animations {

  def error(msg: => Any): Unit = println(errorColor(msg))
  def info(msg: => Any): Unit = println(infoColor(msg))
  def println(msg: => Any): Unit = Console.println(msg)
  def debug(msg: => Any): Unit

  private val SeparatorLine = "-" * 80

  def fmtBlock[A](title: => String)(body: => A): A = {
    val separator = SeparatorLine.drop(title.size + 1)
    println(separatorColor(s"$title $separator"))
    val result = body
    println(s"${separatorColor(SeparatorLine)}\n\n")
    result
  }

}

/**
  * Format default terminal output with verbose disabled and ansi colors disabled.
  */
object InfoFormatter extends CustomFormatter(ansiEnabled = false, verbose = false)

/**
  * Format terminal output with verbose enabled and ansi colors enabled.
  */
case class CustomFormatter(ansiEnabled: Boolean, verbose: Boolean) extends Formatter {
  def debug(msg: => Any): Unit = if (verbose) println(s" [debug] $msg") else {}
}

trait AnsiFormatter {
  def ansiEnabled: Boolean

  // Default colors mapping
  val InfoColor = Console.GREEN
  val ErrorColor = Console.RED
  val ResetColor = Console.RESET
  val SeparatorColor = Console.BLUE
  val ImportantColor = Console.RED

  val Ok = s"$InfoColor\u2713$ResetColor"
  val Fail = s"$ImportantColor\u2715$ResetColor"

  private val EraseLine = "\r\b"

  def importantColor(body: => Any) = {
    if (ansiEnabled) s"$ImportantColor$body$ResetColor" else body
  }

  def infoColor(body: => Any) = {
    if (ansiEnabled) s"$InfoColor$body$ResetColor" else body
  }

  def errorColor(body: => Any) = {
    if (ansiEnabled) s"$ErrorColor$body$ResetColor" else body
  }

  protected def separatorColor(body: => Any) = {
    if (ansiEnabled) s"$SeparatorColor$body$ResetColor" else body
  }

  protected def eraseLine() = {
    print(EraseLine)
  }

  protected def updateLine(msg: => String) = {
    print(s"$EraseLine $msg")
  }
}

trait Animations extends AnsiFormatter {

  private val Sleep = 300
  private val frames = "|/-\\".toCharArray

  /**
    * Show while body is not None
    */
  @tailrec
  final def showAnimated(
      fetchMessage: () => Option[String],
      step: Int = 0
  ): Unit = {
    fetchMessage() match {
      case None =>
        eraseLine()
        println("\n")
      case Some(msg) =>
        val animationFrame = frames(step % frames.size)
        updateLine(s"$animationFrame $msg")
        Thread.sleep(Sleep)
        showAnimated(fetchMessage, step + 1)
    }
  }
}
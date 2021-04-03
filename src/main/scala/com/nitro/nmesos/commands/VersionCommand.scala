package com.nitro.nmesos.commands

import com.nitro.nmesos.BuildInfo

/**
  * Show the nmesos version
  */
case class VersionCommand() extends Command {

  override def run(): CommandResult = {
    CommandSuccess(s"Version: ${BuildInfo.version}")
  }

}

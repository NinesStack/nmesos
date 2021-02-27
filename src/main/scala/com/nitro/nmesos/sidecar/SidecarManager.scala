package com.nitro.nmesos.sidecar

import com.nitro.nmesos.util.{HttpClientHelper, Logger}

case class Service(
    ID: String,
    Name: String,
    Status: Int,
    Hostname: String,
    Image: String
)

case class SidecarServices(
    Services: Map[String, Seq[Service]],
    hostName: String = ""
)

case class SidecarManager(log: Logger) extends HttpClientHelper {

  def getServices(host: String) = {
    get[SidecarServices](s"http://$host:7777/services.json")
      .map(_.map(_.copy(hostName = host)))
  }
}

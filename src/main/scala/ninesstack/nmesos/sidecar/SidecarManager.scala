package ninesstack.nmesos.sidecar

import ninesstack.nmesos.util.{HttpClientHelper, Formatter}
import ninesstack.nmesos.util.CustomPicklers.OptionPickler.{ReadWriter => RW, macroRW}

case class Service(
    ID: String,
    Name: String,
    Status: Int,
    Hostname: String,
    Image: String
)

object Service {
  implicit val rw: RW[Service] = macroRW
}

case class SidecarServices(
    Services: Map[String, Seq[Service]],
    hostName: String = ""
)

object SidecarServices {
  implicit val rw: RW[SidecarServices] = macroRW
}

case class SidecarManager(fmt: Formatter) extends HttpClientHelper {

  def getServices(host: String) = {
    get[SidecarServices](s"http://$host:7777/services.json")
      .map(_.map(_.copy(hostName = host)))
  }
}

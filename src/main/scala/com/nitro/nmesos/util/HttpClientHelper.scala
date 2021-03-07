package com.nitro.nmesos.util

import com.nitro.nmesos.singularity.model.SingularityRequestParent

import scala.util.Try
import scalaj.http._

/**
  * HTTP boilerplate.
  * Note: Http connections are synchronous.
  */
trait HttpClientHelper {
  def log: Logger

  // Custom json reader/writer
  import CustomPicklers.OptionPickler._

  protected def get[A: Reader](url: String): Try[Option[A]] =
    Try {
      logRequest("GET", url)

      val response = send(HttpClient(url))

      if (response.isSuccess) {
        Some(parseBody[A](url, response))
      } else if (response.is4xx) {
        None // NotFound
      } else {
        failure(url, response)
      }
    }

  protected def post[A: Writer, B: Reader](url: String, item: A): Try[B] =
    Try {
      val json = write(item)
      logRequest("POST", url, Some(json))

      val response = send(
        HttpClient(url)
          .header("content-type", "application/json")
          .postData(json)
      )

      if (response.isSuccess) {
        parseBody[B](url, response)
      } else {
        failure(url, response)
      }
    }

  protected def put[A: Writer, B: Reader](url: String, item: A): Try[B] =
    Try {
      val json = write(item)
      logRequest("PUT", url, Some(json))

      val response = send(
        HttpClient(url)
          .header("content-type", "application/json")
          .put(json)
      )

      if (response.isSuccess) {
        parseBody[B](url, response)
      } else {
        failure(url, response)
      }
    }

  /**
    * Try to parse or return a clean error.
    * In verbose mode: log response and errors in console
    */
  private def parseBody[A: Reader](
      url: String,
      response: HttpResponse[String]
  ) = {
    try {
      log.debug(s"Response code: ${response.code}, body: ${response.body}\n")
      read[A](response.body)
    } catch {
      case ex: Exception =>
        log.debug(s"Parser error: $ex")
        sys.error(s"Unable to parse HTTP response - $url")
    }
  }

  private def send(request: HttpRequest): HttpResponse[String] =
    Try {
      request.asString
    }.getOrElse(sys.error(s"Unable to connect to ${request.url}"))

  /**
    * Log HTTP request in verbose mode
    */
  private def logRequest(
      method: String,
      url: String,
      jsonOpt: Option[String] = None
  ): Unit = {

    jsonOpt.foreach { data =>
      log.debug(s"data to send: $data\n")
    }

    def data = {
      jsonOpt
        .map(json => json.replaceAll("\"", "\\\\\""))
        .map(encoded =>
          s"""-H "Content-Type: application/json" -d "$encoded" """
        )
        .getOrElse("")
    }

    log.debug(s"curl -v -X $method $url $data\n")
  }

  /**
    * Log HTTP response in verbose mode return clean error
    */
  private def failure(url: String, response: HttpResponse[String]) = {
    //show only a few lines. plain error can be a large html
    val msg = response.body.take(500)
    log.debug(s"Response > ${response.code}: $msg...\n")
    sys.error(s"HTTP ${response.statusLine} - ${response.code}: $url\n $msg")
  }
}

/**
  * Json boilerplate.
  * - Parse scala Options.
  * - Parse custom Long format from Singularity
  */
object CustomPicklers {

  import upickle.Js

  object OptionPickler extends upickle.AttributeTagged {

    // Support for option of custom case classes
    implicit val customSingularityRequestParentReader = OptionR[Option[SingularityRequestParent]]

    override implicit def OptionW[T: Writer]: Writer[Option[T]] =
      Writer {
        case None    => Js.Null
        case Some(s) => implicitly[Writer[T]].write(s)
      }

    override implicit def OptionR[T: Reader]: Reader[Option[T]] =
      Reader {
        case Js.Null     => None
        case v: Js.Value => Some(implicitly[Reader[T]].read.apply(v))
      }

    /**
      * Hack to parse no standard json Long values.
      * Singularity doesn't return long as strings.
      */
    implicit def long2Reader: Reader[Long] =
      OptionPickler.Reader[Long] {
        case Js.Num(str) =>
          str.toLong
      }
  }

}

object HttpClient extends BaseHttp(userAgent = s"nmesos") {

  lazy val user = Option(System.getProperty("user.name")).getOrElse("unknown")

  override def apply(url: String) =
    super
      .apply(url)
      .header("content-type", "application/json")
      .header("X-Username", user)
}

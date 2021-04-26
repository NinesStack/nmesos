package com.nitro.nmesos.util

import com.nitro.nmesos.singularity.model.SingularityRequestParent

import scala.util.Try
import scalaj.http._

import CustomPicklers.OptionPickler._

/**
  * HTTP boilerplate.
  * Note: Http connections are synchronous.
  */
trait HttpClientHelper {
  private val logger = org.log4s.getLogger

  def fmt: Formatter

  protected def ping(url: String): Try[Option[Unit]] =
    Try {
      logRequest("GET", url)

      val response = send(HttpClient(url))

      if (response.isSuccess) {
        Some(())
      } else if (response.is4xx) {
        None // NotFound
      } else {
        failure(url, response)
      }
    }

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
    */
  private def parseBody[A: Reader](
      url: String,
      response: HttpResponse[String]
  ) = {
    try {
      logger.info(s"Response code: ${response.code}, body: ${response.body}\n")
      read[A](response.body)
    } catch {
      case ex: Exception =>
        logger.info(s"Parser error: $ex")
        sys.error(s"Unable to parse HTTP response - $url")
    }
  }

  private def send(request: HttpRequest): HttpResponse[String] =
    Try {
      request.asString
    }.getOrElse(sys.error(s"Unable to connect - ${request.url}"))

  /**
    * Log HTTP request
    */
  private def logRequest(
      method: String,
      url: String,
      jsonOpt: Option[String] = None
  ): Unit = {

    jsonOpt.foreach { data =>
      logger.info(s"data to send: $data\n")
    }

    def data = {
      jsonOpt
        .map(json => json.replaceAll("\"", "\\\\\""))
        .map(encoded =>
          s"""-H "Content-Type: application/json" -d "$encoded" """
        )
        .getOrElse("")
    }

    logger.info(s"curl -v -X $method $url $data\n")
  }

  /**
    * Log HTTP response
    */
  private def failure(url: String, response: HttpResponse[String]) = {
    //show only a few lines. plain error can be a large html
    val msg = response.body.take(500)
    logger.info(s"Response > ${response.code}: $msg...\n")
    sys.error(s"HTTP ${response.statusLine} - ${response.code}: $url\n $msg")
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

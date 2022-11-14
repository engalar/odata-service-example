package odata

import java.io.ByteArrayInputStream
import akka.util.ByteString
import org.apache.olingo.commons.api.format.ContentType
import org.apache.olingo.commons.api.http.{HttpHeader, HttpMethod, HttpStatusCode}
import org.apache.olingo.server.api.{OData, ODataHandler, ODataRequest, ODataResponse, ServiceMetadata}
import org.apache.olingo.server.core.{ODataHandlerImpl, ODataImpl}
import org.apache.olingo.server.core.debug.ServerCoreDebugger
import play.api.http.{HttpEntity, MediaRange}
import play.api.mvc.{AnyContent, Headers, Request, ResponseHeader, Result}

import scala.jdk.CollectionConverters._
import scala.util.Try

trait PlayODataRequestHandler {
  def handle(playRequest: Request[AnyContent], oDataPath: String): Result
}

class PlayOData4Impl extends ODataImpl {
  override def createRawHandler(serviceMetadata: ServiceMetadata): ODataHandler =
    new PlayODataHandler(this, serviceMetadata)
}

class PlayODataHandler(odata: OData, serviceMetadata: ServiceMetadata)
    extends ODataHandlerImpl(odata, serviceMetadata, new ServerCoreDebugger(odata))
    with PlayODataRequestHandler {

  val METADATA_PATH = "$metadata"

  def toPlayResult(oDataResponse: ODataResponse): Result = {
    val headers = Headers.apply(
      oDataResponse.getAllHeaders.asScala.toSeq
        .filter(_._1.toLowerCase != "content-type")
        .flatMap(h => h._2.asScala.map(v => (h._1, v))): _*
    )
    val contentType = Option(oDataResponse.getHeader(HttpHeader.CONTENT_TYPE))
    val httpEntity  = HttpEntity.Strict(ByteString.apply(oDataResponse.getContent.readAllBytes()), contentType)
    Result(ResponseHeader(oDataResponse.getStatusCode, headers.toSimpleMap, None), httpEntity)
  }

  private def handleMetadataPath(contentType: ContentType): ODataResponse = {
    val serializer = odata.createSerializer(contentType)

    val metadataDocument  = serializer.metadataDocument(serviceMetadata)
    val serializedContent = metadataDocument.getContent

    // Finally: configure the response object: set the body, headers and status code
    val response = new ODataResponse()
    response.setContent(serializedContent)
    response.setStatusCode(HttpStatusCode.OK.getStatusCode)
    response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString)

    response
  }

  private def extractAcceptedType(acceptedTypes: Seq[MediaRange]): ContentType = {
    val supportedContentTypes =
      acceptedTypes
        .flatMap(at => Try(ContentType.parse(s"${at.mediaType}/${at.mediaSubType}")).toOption)
        .filter(ct => ct == ContentType.APPLICATION_JSON || ct == ContentType.APPLICATION_XML)

    if (supportedContentTypes.isEmpty) ContentType.APPLICATION_JSON
    else supportedContentTypes.head
  }

  override def handle(playRequest: Request[AnyContent], oDataPath: String): Result = {
    val oDataResponse = if (oDataPath == METADATA_PATH) {
      handleMetadataPath(extractAcceptedType(playRequest.acceptedTypes))
    } else {
      val oDataRequest = new ODataRequest()
      val routePrefix  = playRequest.path.substring(1, playRequest.path.indexOf(oDataPath))
      val schema       = if (playRequest.secure) "https" else "http"
      val baseUrl      = s"${schema}://${playRequest.host}"

      oDataRequest.setRawRequestUri(playRequest.uri)
      oDataRequest.setRawODataPath(oDataPath)
      oDataRequest.setRawBaseUri(s"$baseUrl/$routePrefix")
      oDataRequest.setMethod(HttpMethod.valueOf(playRequest.method))
      oDataRequest.setRawQueryPath(playRequest.rawQueryString)
      playRequest.headers.toMap.map {
        case (name, value) =>
          oDataRequest.addHeader(name, value.asJava)
      }

      // TODO: There is probably a better way with custom body parser
      // Since we are only using GET requests, this will not present a problem
      oDataRequest.setBody(
        new ByteArrayInputStream(
          playRequest.body.asRaw
            .map(b => b.asBytes(Int.MaxValue).map(_.asByteBuffer.array()).getOrElse(Array.emptyByteArray))
            .getOrElse(Array.emptyByteArray)
        )
      )

      val result = super.process(oDataRequest)
      result
    }

    toPlayResult(oDataResponse)
  }
}

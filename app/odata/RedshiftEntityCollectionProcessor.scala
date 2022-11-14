package odata

import odata.dataproviders.ODataDataProvider
import org.apache.olingo.commons.api.format.ContentType
import org.apache.olingo.server.api.processor.EntityCollectionProcessor
import org.apache.olingo.server.api.uri.UriInfo
import org.apache.olingo.server.api.{ODataRequest, ODataResponse}

import scala.jdk.CollectionConverters._

//Due to java interop
//scalastyle:off null
class RedshiftEntityCollectionProcessor(dataProvider: ODataDataProvider) extends EntityCollectionProcessor {

  import org.apache.olingo.server.api.{OData, ServiceMetadata}

  private var odata: OData                     = null
  private var serviceMetadata: ServiceMetadata = null

  import org.apache.olingo.server.api.{OData, ServiceMetadata}

  def init(odata: OData, serviceMetadata: ServiceMetadata): Unit = {
    this.odata = odata
    this.serviceMetadata = serviceMetadata
  }

  override def readEntityCollection(request: ODataRequest, response: ODataResponse, uriInfo: UriInfo, responseFormat: ContentType): Unit = {
    import org.apache.olingo.commons.api.data.ContextURL
    import org.apache.olingo.commons.api.http.{HttpHeader, HttpStatusCode}
    import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions
    import org.apache.olingo.server.api.uri.UriResourceEntitySet
    // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
    val resourcePaths = uriInfo.getUriResourceParts
    val selectOption  = Option(uriInfo.getSelectOption)
    val filterOption  = Option(uriInfo.getFilterOption).map(f => f.getExpression.accept(new FilterExpressionRedshiftVisitor()))

    val uriResourceEntitySet = resourcePaths.get(0).asInstanceOf[UriResourceEntitySet] // in our example, the first segment is the EntitySet
    val edmEntitySet         = uriResourceEntitySet.getEntitySet
    val keyPredicates        = uriResourceEntitySet.getKeyPredicates.asScala

    // 2nd: fetch the data from backend for this requested EntitySetName
    // it has to be delivered as EntitySet object

    val edmEntityType = edmEntitySet.getEntityType
    val selectList    = odata.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption.orNull)
    val contextUrl    = ContextURL.`with`.entitySet(edmEntitySet).selectList(selectList).build

    // 3rd: create a serializer based on the requested format (json)
    val serializer = odata.createSerializer(responseFormat)

    val entitySet = dataProvider.getData(edmEntitySet, Option(selectList), filterOption)

    val id                = request.getRawBaseUri + edmEntitySet.getName
    val opts              = EntityCollectionSerializerOptions.`with`.id(id).contextURL(contextUrl).select(selectOption.orNull).build
    val serializerResult  = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts)
    val serializedContent = serializerResult.getContent

    // Finally: configure the response object: set the body, headers and status code
    response.setContent(serializedContent)
    response.setStatusCode(HttpStatusCode.OK.getStatusCode)
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString)
  }

}

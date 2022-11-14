package controllers

import com.zaxxer.hikari.HikariDataSource
import odata._
import odata.dataproviders.ODataDataProvider
import org.apache.olingo.commons.api.edmx.EdmxReference
import play.api.Logging
import play.api.mvc._

import javax.inject._
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class ODataController @Inject() (cc: ControllerComponents, dataProvider: ODataDataProvider)(implicit
    ec: ExecutionContext,
    dataSource: HikariDataSource
) extends AbstractController(cc)
    with Logging {

  def get(databaseName: String, tableName: String): Action[AnyContent] =
    Action { implicit request =>
      Try { // Using try due to https://github.com/playframework/playframework/issues/10486
        val oData         = new PlayOData4Impl()
        val edmProvider   = new RedshiftEdmProvider(databaseName)
        val edmxReference = List[EdmxReference]().asJava
        // Currently generated based on request, but should be cached/generated once user decides to expose the data set
        val serviceMetadata = oData.createServiceMetadata(edmProvider, edmxReference)
        val handler         = oData.createRawHandler(serviceMetadata).asInstanceOf[PlayODataHandler]
        handler.register(new RedshiftEntityCollectionProcessor(dataProvider))

        handler.handle(request, tableName)
      } match {
        case Success(r) =>
          r
        case Failure(f) =>
          logger.error("Error during request processing", f)
          InternalServerError(f.getMessage)
      }
    }
}

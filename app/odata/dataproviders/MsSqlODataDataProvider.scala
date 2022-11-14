package odata.dataproviders

import com.zaxxer.hikari.HikariDataSource
import odata.{JdbcHelpers, SqlDataAccess}

import javax.inject.Inject


class MsSqlODataDataProvider @Inject()(implicit val dataSource: HikariDataSource) extends ODataDataProvider {

  import org.apache.olingo.commons.api.data.EntityCollection
  import org.apache.olingo.commons.api.edm.EdmEntitySet

  private def translateODataQueryToSql(edmEntitySet: EdmEntitySet, selectList: Seq[String], filter: Option[String]): String = {
    val databaseName = edmEntitySet.getEntityContainer.getName
    val tableName = edmEntitySet.getName
    val limit = "TOP 100"
    val fieldsToSelect = if (selectList.isEmpty) "*" else selectList.mkString(",")
    val selectStatement = s"SELECT ${limit} $fieldsToSelect FROM $databaseName.dbo.$tableName"//replace dbo with public for postgres/redshift db
    val filterStatement = if (filter.isEmpty) "" else s"WHERE ${filter.get}"
    val orderByStatement = ""


    val query = s"${selectStatement} ${filterStatement} ${orderByStatement}"
    println(query)
    query
  }

  def getData(edmEntitySet: EdmEntitySet, selectList: Option[String], filter: Option[String]): EntityCollection = {
    val selectFields = selectList match {
      case Some(s) => s.split(',').toSeq
      case None => Seq.empty
    }

    import SqlDataAccess._

    val selectStatement = translateODataQueryToSql(edmEntitySet, selectFields, filter)

    usingConnection { implicit conn =>
      withStatement(selectStatement) { preparedStatement =>
        val queryResults = preparedStatement.executeQuery()
        JdbcHelpers.toEntityCollection(queryResults, edmEntitySet, selectFields)
      }
    } match {
      case Right(r) => r
      case Left(t) => throw t
    }
  }
}

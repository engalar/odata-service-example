package odata

import com.zaxxer.hikari.HikariDataSource
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.apache.olingo.commons.api.edm.provider._

import scala.jdk.CollectionConverters._

object RedshiftEdmProvider {
  val NAMESPACE_BASE = "com.example.data"
  val CONTAINER_NAME = "Container"
}

class RedshiftEdmProvider(databaseName: String)(implicit dataSource: HikariDataSource) extends CsdlAbstractEdmProvider {

  val normalizedDatabaseName = databaseName.toLowerCase

  import RedshiftEdmProvider._

  val NAMESPACE = s"$NAMESPACE_BASE.$normalizedDatabaseName"
  val CONTAINER = new FullQualifiedName(NAMESPACE, normalizedDatabaseName)

  val jdbcToEdmEntity = new JdbcToEdmEntity()

  val entityTypes = convertJdbcTableToEdmEntityType(databaseName).toMap
  val entitySets  = entityTypes.keys.map(k => k -> createEntitySet(k)).toMap

  val entityContainer = new CsdlEntityContainer()
  entityContainer.setName(normalizedDatabaseName)
  entityContainer.setEntitySets(entitySets.values.toSeq.asJava)

  val schema = new CsdlSchema()
  schema.setNamespace(NAMESPACE)
  schema.setEntityContainer(entityContainer)
  schema.setEntityTypes(entityTypes.keys.map(getEntityType).toSeq.asJava)

  val schemas = Seq(schema)

  private def createEntitySet(entityType: FullQualifiedName): CsdlEntitySet = {
    val entitySet = new CsdlEntitySet
    entitySet.setName(entityType.getName)
    entitySet.setType(entityType)
    entitySet
  }

  private def convertJdbcTableToEdmEntityType(
      databaseName: String
  ): Seq[(FullQualifiedName, CsdlEntityType)] = {

    val databaseMetaData = JdbcHelpers.getDatabaseMetadata
    val jdbcSchemas      = jdbcToEdmEntity.toTableSchema(databaseMetaData, databaseName, None)

    def schemaToCsdlEntityType(entityTypeName: FullQualifiedName, schema: TableSchema): CsdlEntityType = {
      jdbcToEdmEntity.toEdmEntity(entityTypeName, schema) match {
        case Right(entity) =>
          entity
        case Left(error) =>
          throw error
      }
    }

    jdbcSchemas.map {
      case tableSchema =>
        val fqn            = new FullQualifiedName(s"$NAMESPACE.${tableSchema.tableName.toLowerCase}")
        val csdlEntityType = schemaToCsdlEntityType(fqn, tableSchema)
        (fqn, csdlEntityType)
    }
  }

  override def getEntityType(entityTypeName: FullQualifiedName): CsdlEntityType = {
    entityTypes
      .get(entityTypeName)
      .orNull
  }

  override def getEntitySet(entityContainer: FullQualifiedName, entitySetName: String): CsdlEntitySet =
    entitySets.get(new FullQualifiedName(s"${entityContainer.getNamespace}.$entitySetName")).orNull

  override def getEntityContainer: CsdlEntityContainer =
    entityContainer

  import java.util

  override def getSchemas: util.List[CsdlSchema] =
    schemas.asJava

  override def getEntityContainerInfo(entityContainerName: FullQualifiedName): CsdlEntityContainerInfo = {
    // This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService.svc/
    val entityContainerInfo = new CsdlEntityContainerInfo()
    entityContainerInfo.setContainerName(CONTAINER)
    entityContainerInfo
  }
}

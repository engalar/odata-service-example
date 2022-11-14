package odata

import com.zaxxer.hikari.HikariDataSource

import java.sql.{DatabaseMetaData, ResultSet, Timestamp}
import org.apache.olingo.commons.api.data.{Entity, EntityCollection, Property, ValueType}
import org.apache.olingo.commons.api.edm.{EdmEntitySet, EdmPrimitiveTypeKind}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object JdbcHelpers {

  def getDatabaseMetadata(implicit dataSource: HikariDataSource): DatabaseMetaData = {
    import SqlDataAccess._

    usingConnection { connection =>
      Try { connection.getMetaData }.toEither
    } match {
      case Right(r) => r
      case Left(t)  => throw t
    }
  }

  def toEntityCollection(resultSet: ResultSet, edmEntitySet: EdmEntitySet, selectedFields: Seq[String]): EntityCollection = {
    val entityCollection = new EntityCollection()
    val entities         = entityCollection.getEntities
    val entityType       = edmEntitySet.getEntityType
    val entityProperties = edmEntitySet.getEntityType.getPropertyNames.asScala.map(_.toLowerCase)
    val propertiesNames = if (selectedFields.isEmpty) {
      entityProperties
    } else selectedFields.flatMap(f => entityProperties.find(_ == f.toLowerCase))

    //scalastyle:off cyclomatic.complexity
    //scalastyle:off null
    def iterate() =
      new Iterator[Entity] {
        def hasNext: Boolean = resultSet.next()

        def next: Entity = {
          val entity = new Entity()
          propertiesNames.map { propertyName =>
            var value: Any       = null
            val property         = entityType.getProperty(propertyName)
            val propertyTypeName = property.getType.getFullQualifiedName
            if (propertyTypeName == EdmPrimitiveTypeKind.Int32.getFullQualifiedName)
              value = resultSet.getInt(propertyName)
            else if (propertyTypeName == EdmPrimitiveTypeKind.Int64.getFullQualifiedName) {
              value = resultSet.getLong(propertyName)
            } else if (propertyTypeName == EdmPrimitiveTypeKind.Boolean.getFullQualifiedName)
              value = resultSet.getBoolean(propertyName)
            else if (propertyTypeName == EdmPrimitiveTypeKind.String.getFullQualifiedName)
              value = resultSet.getString(propertyName)
            else if (propertyTypeName == EdmPrimitiveTypeKind.Double.getFullQualifiedName)
              value = resultSet.getDouble(propertyName)
            else if (propertyTypeName == EdmPrimitiveTypeKind.Decimal.getFullQualifiedName)
            value = resultSet.getBigDecimal(propertyName)
            else if (propertyTypeName == EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName)
              value = Try { resultSet.getTimestamp(propertyName) } match {
                case Failure(f) => Timestamp.from(java.time.Instant.ofEpochMilli(resultSet.getLong(propertyName)))
                case Success(v) => v
              }
            else throw new Exception(s"Type $propertyTypeName is not supported!")

            entity.addProperty(new Property(null, propertyName, ValueType.PRIMITIVE, value))
          }

          entities.add(entity)
          entity
        }
      }.toList

    iterate()

    entityCollection
  }
}

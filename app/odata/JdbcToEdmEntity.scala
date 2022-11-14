package odata

import org.apache.olingo.commons.api.edm.{EdmPrimitiveTypeKind, FullQualifiedName}
import org.apache.olingo.commons.api.edm.provider.{CsdlEntityType, CsdlProperty, CsdlPropertyRef}

import java.sql.{DatabaseMetaData, JDBCType}
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.util.Try

case class ColumnSchema(name: String, dataType: JDBCType, isNullable: Boolean, isKeyPart: Boolean)

case class TableSchema(databaseName: String, schemaName: Option[String], tableName: String, columns: Seq[ColumnSchema])

//Because of java interop, we disable some standard scala checks
//scalastyle:off null
class JdbcToEdmEntity {

  val USER_DEFINED_TABLES = "TABLE"
  val USER_DEFINED_VIEWS  = "VIEW"

  val COLUMN_NAME = "COLUMN_NAME"
  val DATA_TYPE   = "DATA_TYPE"
  val IS_NULLABLE = "IS_NULLABLE"

  def toTableSchema(metadata: DatabaseMetaData, databaseName: String, schemaName: Option[String]): Seq[TableSchema] = {
    val tablesResultSet = metadata.getTables(databaseName, schemaName.orNull, null, Array(USER_DEFINED_TABLES))
    val tables          = ArrayBuffer[TableSchema]()

    while (tablesResultSet.next()) {
      val tableName         = tablesResultSet.getString("TABLE_NAME")
      val primaryKeyColumns = getPrimaryKeys(metadata, databaseName, schemaName, tableName)
      val columns           = getColumnsForTable(metadata, databaseName, schemaName, tableName, primaryKeyColumns)
      val tableSchema       = TableSchema(databaseName, schemaName, tableName, columns)
      tables.append(tableSchema)
    }

    tables.toSeq
  }

  private def getColumnsForTable(
      metadata: DatabaseMetaData,
      databaseName: String,
      schemaName: Option[String],
      tableName: String,
      primaryKeyColumns: Seq[String]
  ): Seq[ColumnSchema] = {
    val columnsResultSet = metadata.getColumns(databaseName, schemaName.orNull, tableName, null)
    val columns          = ArrayBuffer[ColumnSchema]()

    while (columnsResultSet.next()) {
      val name             = columnsResultSet.getString(COLUMN_NAME)
      val dataType         = java.sql.JDBCType.valueOf(columnsResultSet.getInt(DATA_TYPE))
      val isNullable       = java.lang.Boolean.parseBoolean(columnsResultSet.getString(IS_NULLABLE))
      val isPrimaryKeyPart = primaryKeyColumns.exists(c => c == name)
      val columnSchema     = ColumnSchema(name, dataType, isNullable, isPrimaryKeyPart)
      columns.append(columnSchema)
    }

    columns.toSeq
  }

  private def getPrimaryKeys(
      metadata: DatabaseMetaData,
      databaseName: String,
      schemaName: Option[String],
      tableName: String
  ): Seq[String] = {
    val primaryKeysResultSet = metadata.getPrimaryKeys(databaseName, schemaName.orNull, tableName)
    val primaryKeyColumns    = ArrayBuffer[String]()
    while (primaryKeysResultSet.next()) {
      val keyColumnName = primaryKeysResultSet.getString(COLUMN_NAME)
      primaryKeyColumns.append(keyColumnName)
    }

    primaryKeyColumns.toSeq
  }

  /** *
    * Converts internal TableSchema representation to CsdlEntityType required by Olingo library
    *
    * @param entityTypeName
    * @param schema
    * @return
    */
  def toEdmEntity(entityTypeName: FullQualifiedName, schema: TableSchema): Either[Throwable, CsdlEntityType] =
    Try {
      val primaryKeyProperties = ArrayBuffer[CsdlPropertyRef]()
      val properties = schema.columns
        .foldRight(Seq.empty[CsdlProperty]) { (column, acc) =>
          val prop = new CsdlProperty()
            .setName(column.name.toLowerCase)
            .setType(getEdmPrimitiveType(column.dataType).getFullQualifiedName)
            .setNullable(column.isNullable)

          if (column.isKeyPart) primaryKeyProperties.append(new CsdlPropertyRef().setName(prop.getName))

          prop +: acc
        //TODO: Handle special types
        }

      val entityType = new CsdlEntityType()
      entityType.setName(entityTypeName.getName.toLowerCase)
      entityType.setProperties(properties.asJava)
      entityType.setKey(primaryKeyProperties.asJava)
      entityType

    }.toEither

  //scalastyle:off cyclomatic.complexity
  private def getEdmPrimitiveType(jdbcType: JDBCType): EdmPrimitiveTypeKind = {
    jdbcType match {
      case JDBCType.BOOLEAN                 => EdmPrimitiveTypeKind.Boolean
      case JDBCType.INTEGER                 => EdmPrimitiveTypeKind.Int32
      case JDBCType.BIGINT                  => EdmPrimitiveTypeKind.Int64
      case JDBCType.FLOAT                   => EdmPrimitiveTypeKind.Double
      case JDBCType.DOUBLE                  => EdmPrimitiveTypeKind.Double
      case JDBCType.VARCHAR                 => EdmPrimitiveTypeKind.String
      case JDBCType.BINARY                  => EdmPrimitiveTypeKind.Binary
      case JDBCType.DATE                    => EdmPrimitiveTypeKind.Date
      case JDBCType.TIME                    => EdmPrimitiveTypeKind.TimeOfDay
      case JDBCType.TIMESTAMP               => EdmPrimitiveTypeKind.DateTimeOffset
      case JDBCType.TIMESTAMP_WITH_TIMEZONE => EdmPrimitiveTypeKind.DateTimeOffset
      case JDBCType.DECIMAL                 => EdmPrimitiveTypeKind.Decimal
      case JDBCType.NUMERIC                 => EdmPrimitiveTypeKind.Decimal
      case JDBCType.BIT                     => EdmPrimitiveTypeKind.Boolean
      case JDBCType.NVARCHAR                => EdmPrimitiveTypeKind.String
      case JDBCType.CHAR                    => EdmPrimitiveTypeKind.String
      case JDBCType.SMALLINT                => EdmPrimitiveTypeKind.Int16
      case JDBCType.TINYINT                 => EdmPrimitiveTypeKind.Int16
      case JDBCType.LONGNVARCHAR            => EdmPrimitiveTypeKind.String
      case JDBCType.LONGVARCHAR             => EdmPrimitiveTypeKind.String
      case JDBCType.NCHAR                   => EdmPrimitiveTypeKind.String
      case JDBCType.VARBINARY               => EdmPrimitiveTypeKind.Binary

      case other                            => throw new Exception(s"Unsupported jdbc type $other")
    }
  }

}

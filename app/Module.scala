import com.google.inject.{AbstractModule, Provides}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import net.codingwell.scalaguice.ScalaModule
import odata.dataproviders.{MsSqlODataDataProvider, ODataDataProvider, ODataProviderSettings, RedshiftODataDataProvider}
import play.api.Configuration

import javax.inject.Singleton

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule with ScalaModule {

  override def configure() = {
    super.configure()
  }

  private def createMsSqlODataProviderSettings(config: Configuration): ODataProviderSettings = {
    val connectionString = config.get[String]("odata.data_providers.mssql.connection_string")
    val username = config.get[String]("odata.data_providers.mssql.username")
    val password = config.get[String]("odata.data_providers.mssql.password")

    ODataProviderSettings(connectionString, username, password, "mssql")
  }

  private def createRedshiftODataProviderSettings(config: Configuration): ODataProviderSettings = {
    val connectionString = config.get[String]("odata.data_providers.redshift.connection_string")
    val username = config.get[String]("odata.data_providers.redshift.username")
    val password = config.get[String]("odata.data_providers.redshift.password")

    ODataProviderSettings(connectionString, username, password, "redshift")
  }

  @Provides
  @Singleton
  private def provideAthenaODataDataProvider(config: Configuration): ODataProviderSettings = {
    val providerType = config.get[String]("odata.data_providers.type")

    providerType match {
      case "mssql" => createMsSqlODataProviderSettings(config)
      case "redshift" => createRedshiftODataProviderSettings(config)
      case t => throw new RuntimeException(s"Unexpected provider type: $t")
    }
  }

  @Provides
  @Singleton
  private def provideHikariDataSource(settings: ODataProviderSettings): HikariDataSource = {
    val hikariConfig = new HikariConfig
    hikariConfig.setJdbcUrl(settings.connectionString)
    hikariConfig.setMaximumPoolSize(20)
    hikariConfig.setUsername(settings.username)
    hikariConfig.setPassword(settings.password)
   // hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
   // hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
   // hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    hikariConfig.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
    new HikariDataSource(hikariConfig)
  }

  @Provides
  @Singleton
  private def provideODataDataProvider(dataSource: HikariDataSource, providerSettings: ODataProviderSettings): ODataDataProvider = {
    providerSettings.providerType match {
      case "mssql" => new MsSqlODataDataProvider()(dataSource)
      case "redshift" => new RedshiftODataDataProvider()(dataSource)
      case t => throw new RuntimeException(s"Unexpected provider type: $t")
    }
  }
}

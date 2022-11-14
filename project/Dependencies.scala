import sbt._

object Dependencies {
  object Play {

    import play.sbt.PlayImport._

    private lazy val scalaGuice = "net.codingwell" %% "scala-guice" % "4.2.6"

    lazy val libs = Seq(guice, logback, filters, scalaGuice)
  }

  object App {
    val olingoVersion = "4.8.0"

    private lazy val olingo              = "org.apache.olingo"       % "odata-server-core" % olingoVersion
    private lazy val hikariPool          = "com.zaxxer"              % "HikariCP"          % "3.1.0"
    private lazy val redshiftJdbcDriver  = "com.amazon.redshift"     % "redshift-jdbc42"   % "2.1.0.1"
    private lazy val sqlServerJdbcDriver = "com.microsoft.sqlserver" % "mssql-jdbc"        % "9.4.0.jre11"
    private lazy val jacksonModule =
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1" //Fix for jackson module issues in akka
    //private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.14" //Fix for version incompatibillity

    private[Dependencies] lazy val libs =
      Seq(
        olingo,
        hikariPool,
        jacksonModule,
        redshiftJdbcDriver,
        sqlServerJdbcDriver,
        //akkaHttp
      )
  }

  object ODataService {
    lazy val libs = App.libs ++ Play.libs
  }
}

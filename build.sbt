import sbt.Keys.{name, resolvers}
import sbt.Resolver

lazy val commonSettings = Seq(
  organization := "com.mendix.example",
  scalaVersion := "2.13.5"
)

lazy val appName = "odata-service"

lazy val root = (project in file("."))
  .settings(
    name := appName,
    fork := true,
    libraryDependencies ++= Dependencies.ODataService.libs,
    logBuffered := false
  )
  .settings(
    releaseVersionFile := file("./version.sbt"),
    releaseUseGlobalVersion := false
  )
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala)


scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  Resolver.bintrayRepo("subclipse", "maven"),
  "MuleSoft Releases" at "https://repository.mulesoft.org/nexus/content/repositories/public/",
  "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
)

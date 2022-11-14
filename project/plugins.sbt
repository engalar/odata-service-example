addSbtPlugin("com.eed3si9n"             % "sbt-assembly"          % "0.14.10")
addSbtPlugin("com.github.gseitz"        % "sbt-release"           % "1.0.10")
addSbtPlugin("com.typesafe.sbt"         % "sbt-git"               % "1.0.0")
addSbtPlugin("net.virtual-void"         % "sbt-dependency-graph"  % "0.10.0-RC1")
addSbtPlugin("org.scoverage"            % "sbt-scoverage"         % "1.6.1")
addSbtPlugin("com.typesafe.play"        % "sbt-plugin"            % "2.8.8")
addSbtPlugin("com.geirsson"             % "sbt-scalafmt"          % "1.5.1")
addSbtPlugin("org.scalastyle"          %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.typesafe.sbt"         % "sbt-native-packager"   % "1.3.6")

resolvers ++= Seq(
  Resolver.url("typesafe-ivy-repo", url("https://typesafe.artifactoryonline.com/typesafe/releases"))(Resolver.ivyStylePatterns),
  "spray repo" at "https://repo.spray.io/",
  "Scala Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  Classpaths.sbtPluginReleases
)

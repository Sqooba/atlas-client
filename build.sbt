organization := "io.sqooba"
scalaVersion := "2.11.11"
version      := "0.1.15"
name         := "atlas-client"

crossScalaVersions := Seq("2.11.12", "2.12.7")

val dispatchVersion = "0.13.2"
val json4sVersion = "3.2.11" // match spark2

resolvers ++= Seq(
  "Sqooba libs-release" at "https://artifactory-v2.sqooba.io/artifactory/libs-release",
  "Sqooba sbt" at "https://artifactory-v2.sqooba.io/artifactory/libs-sbt-local/",
  "JBoss" at "https://repository.jboss.org/",
  "HDP Releases" at "http://repo.hortonworks.com/content/repositories/releases/",
  "HDP Releases Public" at "http://repo.hortonworks.com/content/groups/public/",
  Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "net.databinder.dispatch"     %%  "dispatch-core"           % dispatchVersion,
  "org.json4s"                  %%  "json4s-ext"              % json4sVersion,
  "org.json4s"                  %%  "json4s-ast"              % json4sVersion, // included in spark
  "org.json4s"                  %%  "json4s-core"             % json4sVersion, // included in spark
  "org.json4s"                  %%  "json4s-jackson"          % json4sVersion, // included in spark,
  "ch.qos.logback"              %   "logback-classic"         % "1.2.3",
  "io.sqooba"                   %%  "sq-conf"                 % "0.3.5",
  "com.typesafe.scala-logging"  %%  "scala-logging"           % "3.7.2",
  "org.scalatest"               %%  "scalatest"               % "3.0.4"           % Test,
  "org.mockito"                 %   "mockito-all"             % "1.10.19"         % Test
)

dependencyOverrides += "org.json4s" %%  "json4s-core" % "3.2.11"

testOptions in Test += Tests.Argument("-l", "ExternalSpec")

lazy val External = config("ext").extend(Test)
configs(External)
inConfig(External)(Defaults.testTasks)
testOptions in External -= Tests.Argument("-l", "ExternalSpec")
testOptions in External += Tests.Argument("-n", "ExternalSpec")


val artUser = sys.env.get("ARTIFACTORY_CREDS_USR").getOrElse("")
val artPass = sys.env.get("ARTIFACTORY_CREDS_PSW").getOrElse("")

credentials += Credentials("Artifactory Realm", "artifactory-v2.sqooba.io", artUser, artPass)

publishTo := {
  val realm = "Artifactory Realm"
  val artBaseUrl = "https://artifactory-v2.sqooba.io/artifactory"
  if (isSnapshot.value) {
    Some(realm at s"$artBaseUrl/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  } else {
    Some(realm at s"$artBaseUrl/libs-release-local")
  }
}

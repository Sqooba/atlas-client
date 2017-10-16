organization := "io.sqooba"
scalaVersion := "2.12.3"
version      := "0.1.0"
name := "atlas-client"

val dispatchVersion = "0.13.2"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" 	  %%  "dispatch-core"           % dispatchVersion,
  "net.databinder.dispatch" 	  %%  "dispatch-json4s-native"  % dispatchVersion,
  "org.json4s"              	  %%  "json4s-ext"              % "3.5.3",
  "ch.qos.logback"              %   "logback-classic"         % "1.2.3",
  "com.typesafe"                %   "config"                  % "1.3.1",
  "com.typesafe.scala-logging"  %%  "scala-logging"           % "3.7.2",
  "org.scalatest"           	  %% 	"scalatest"               % "3.0.3" 			      % Test,
  "org.mockito"                 %   "mockito-all"             % "1.10.19"           % Test
)

excludeDependencies ++= Seq("org.slf4j" % "slf4j-log4j12", "log4j" % "log4j")

lazy val E2e = config("e2e").extend(Test)
configs(E2e)
inConfig(E2e)(Defaults.testTasks)
testOptions in E2e -= Tests.Argument("-l", "E2E")
testOptions in E2e += Tests.Argument("-n", "E2E")

publishTo := {
  val realm = "Artifactory Realm"
  val artBaseUrl = "https://artifactory.sqooba.io/artifactory"
  if (isSnapshot.value) {
    Some(realm at s"$artBaseUrl/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  } else {
    Some(realm at s"$artBaseUrl/libs-release-local")
  }
}

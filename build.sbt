import sbt.Keys.libraryDependencies
import scala.collection.immutable.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

val logbackVersion = "1.4.7"
val sfl4sVersion = "2.0.5"
val guavaVersion = "31.1-jre"
val typesafeConfigVersion = "1.4.1"
val akkaVersion = "2.7.0"
val akkaHttpVersion = "10.4.0"
val scalaTestVersion = "3.2.11"

lazy val root = (project in file("."))
  .settings(
    name := "PoliceMan-Thief-Graph-Game",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-core" % logbackVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.google.guava" % "guava" % guavaVersion,
      "org.slf4j" % "slf4j-api" % sfl4sVersion,
      "com.typesafe" % "config" % typesafeConfigVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "software.amazon.awssdk" % "s3" % "2.17.21",
      "org.apache.httpcomponents" % "httpclient" % "4.5.13",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %% "mockito-4-2" % "3.2.12.0-RC2" % Test,
      "com.typesafe.akka" %% "akka-testkit" % "2.7.0" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % "10.2.6" % "test"
    )
  )

compileOrder := CompileOrder.JavaThenScala
test / fork := true
run / fork := true
run / connectInput := true
run / javaOptions ++= Seq(
  "-Xms8G",
  "-Xmx100G",
  "-XX:+UseG1GC"
)

Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat

Compile / mainClass := Some("Main")
run / mainClass := Some("Main")

val jarName = "policeman_thief_graph_game.jar"
assembly / assemblyJarName := jarName

// Merging strategies
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
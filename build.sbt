import scala.language.postfixOps

ThisBuild / organization := "com.codegans.decorpyc"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / libraryDependencies ++= Dependencies.loggerDependencies ++ Dependencies.testDependencies

assembly / assemblyJarName := s"${name.value}-${version.value.stripSuffix("-SNAPSHOT")}.jar"
assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x => MergeStrategy.defaultMergeStrategy(x)
}

Compile / mainClass := Some("com.codegans.decorpyc.EntryPoint")
//Compile / renpyVersions := Seq("7.3.5")

lazy val root = (project in file(".")).enablePlugins(RenPyPlugin).settings(
  name := "decorpyc"
)

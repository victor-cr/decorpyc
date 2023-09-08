import sbtrelease.Version

import scala.language.postfixOps

ThisBuild / organization := "com.codegans.decorpyc"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / libraryDependencies ++= Dependencies.compileDependencies ++ Dependencies.runtimeDependencies ++ Dependencies.testDependencies

Compile / mainClass := Some("com.codegans.decorpyc.EntryPoint")
//Compile / renpyVersions := Seq("7.3.5")

lazy val root = (project in file(".")).enablePlugins(RenPyPlugin, JavaAppPackaging).settings(
  name := "decorpyc",
  maintainer := "victor2@ukr.net",
  publish := false,
  releaseVersionBump := Version.Bump.Next,
  releaseVersion := Release.fnReleaseVersion,
  releaseNextVersion := Release.fnNextReleaseVersion(releaseVersionBump.value),
)

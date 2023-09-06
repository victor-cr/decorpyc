import sbtrelease.Version

import scala.language.postfixOps

ThisBuild / organization := "com.codegans.decorpyc"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / libraryDependencies ++= Dependencies.loggerDependencies ++ Dependencies.testDependencies

Compile / mainClass := Some("com.codegans.decorpyc.EntryPoint")
//Compile / renpyVersions := Seq("7.3.5")

lazy val root = (project in file(".")).enablePlugins(RenPyPlugin).settings(
  name := "decorpyc",
  publishTo := Some(Resolver.file("local-ivy", file("./target/release/"))),
  releaseVersionBump := Version.Bump.Next,
  releaseVersion := Release.fnReleaseVersion,
  releaseNextVersion := Release.fnNextReleaseVersion(releaseVersionBump.value),
)

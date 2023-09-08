import sbtrelease.Version

import scala.language.postfixOps

ThisBuild / organization := "com.codegans.decorpyc"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / libraryDependencies ++= Dependencies.compileDependencies ++ Dependencies.runtimeDependencies ++ Dependencies.testDependencies

ThisBuild / publish / skip := true
ThisBuild / publishMavenStyle := true
ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "victor-cr",
  sys.env.getOrElse("GITHUB_TOKEN", "verysecretpassword")
)
ThisBuild / publishTo := Some(
  "GitHub Package Registry " at "https://maven.pkg.github.com/victor-cr/decorpyc"
)

Compile / mainClass := Some("com.codegans.decorpyc.EntryPoint")
//Compile / renpyVersions := Seq("7.3.5")

Universal / topLevelDirectory := Some(name.value)
Universal / packageBin := {
  val originalFileName = (Universal / packageBin).value
  val ext = originalFileName.ext
  val newFileName = file(originalFileName.getParent) / (name.value + "-v" + version.value + "." + ext)
  IO.move(originalFileName, newFileName)
  newFileName
}

lazy val root = (project in file(".")).enablePlugins(RenPyPlugin, JavaAppPackaging).settings(
  name := "decorpyc",
  maintainer := "victor2@ukr.net",
  releaseVersionBump := Version.Bump.Next,
  releaseVersion := Release.fnReleaseVersion,
  releaseNextVersion := Release.fnNextReleaseVersion(releaseVersionBump.value),
)

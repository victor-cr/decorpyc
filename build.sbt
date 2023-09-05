import scala.language.postfixOps

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .enablePlugins(RenPyPlugin)
  .settings(
    name := "decorpyc",
//    Compile / renpyVersions := Seq("7.3.5"),
    libraryDependencies ++= Dependencies.loggerDependencies ++ Dependencies.testDependencies
  )

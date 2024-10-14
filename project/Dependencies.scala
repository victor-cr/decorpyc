import sbt._

object Dependencies {
  lazy val compileDependencies: Seq[ModuleID] = Seq(
    // Default logger API
    "org.slf4j" % "slf4j-api" % "2.0.12" % Compile,
    // Scala CLI API
    "org.rogach" %% "scallop" % "5.1.0" % Compile,
  )

  lazy val runtimeDependencies: Seq[ModuleID] = Seq(
    // Logger backend
    "ch.qos.logback" % "logback-classic" % "1.5.9" % Runtime
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    // Test framework
    "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )
}
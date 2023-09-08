import sbt._

object Dependencies {
  lazy val compileDependencies: Seq[ModuleID] = Seq(
    // Default logger API
    "org.slf4j" % "slf4j-api" % "2.0.5" % Compile,
    // Scala CLI API
    "org.rogach" %% "scallop" % "5.0.0" % Compile,
  )

  lazy val runtimeDependencies: Seq[ModuleID] = Seq(
    // Logger backend
    "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    // Test framework
    "org.scalatest" %% "scalatest" % "3.2.16" % Test
  )
}
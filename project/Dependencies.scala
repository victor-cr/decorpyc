import sbt._

object Dependencies {
  lazy val loggerDependencies: Seq[ModuleID] = Seq(
    //Default logger API
    "org.slf4j" % "slf4j-api" % "2.0.5" % Compile,
    "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
  )

  lazy val testDependencies: Seq[ModuleID] = Seq("org.scalatest" %% "scalatest" % "3.2.16" % Test)
}
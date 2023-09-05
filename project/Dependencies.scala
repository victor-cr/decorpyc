import sbt._

object Dependencies {
  val loggerDependencies = Seq(
    //Default logger API
    "org.slf4j" % "slf4j-api" % "2.0.5" % Compile,
    "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.16" % Test)
}
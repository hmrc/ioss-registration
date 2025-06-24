import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

lazy val microservice = Project("ioss-registration", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion        := 0,
    scalaVersion        := "3.3.4",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
  )
  .settings(PlayKeys.playDefaultPort := 10191)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .configs(Test)
  .settings(inConfig(Test)(testSettings): _*)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")

lazy val testSettings = Defaults.testSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "test",
    baseDirectory.value / "test" / "testutils"
  ),
  parallelExecution := false,
  fork := true,
  javaOptions ++= Seq(
    "-Dlogger.resource=logback-test.xml"
  )
)

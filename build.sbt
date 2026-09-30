import uk.gov.hmrc.DefaultBuildSettings

val appName = "api-gatekeeper-xml-services-frontend"

Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))

ThisBuild / scalaVersion := "3.7.4"
ThisBuild / majorVersion := 0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision


lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    Assets / pipelineStages := Seq(
      concat
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.binders.RouteModels.*",
      "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.binders.RouteModels.given",
    )
  )
  .settings(
    Test / testOptions       += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / fork              := false,
    Test / parallelExecution := false
  )
  .settings(ScoverageSettings())
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components.*",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.*",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers.*"
    )
  )
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=views/.*:s",
      "-Wconf:msg=unused implicit parameter&src=views/.*:s",
      "-Wconf:msg=unused explicit parameter&src=views/.*:s",
      "-Wconf:msg=unused import&src=routes/.*:s",
      "-Wconf:msg=unused pattern variable&src=routes/.*:s",
      "-Wconf:msg=unused private member&src=routes/.*:s",
    )
  )


lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "integration-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    DefaultBuildSettings.itSettings()
  )



commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: state},
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: state},
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: state},
  Command.command("testAll") { state => "test" :: "it/test" :: state},
  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "run-all-tests" :: "coverageOff" :: "coverageAggregate" :: state }
)

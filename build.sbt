import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import bloop.integrations.sbt.BloopDefaults

val appName = "api-gatekeeper-xml-services-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.13",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
        TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    pipelineStages in Assets := Seq(gzip),
  )
  .settings(publishingSettings, scoverageSettings)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings))
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

  lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := ";.*\\.domain\\.models\\..*;uk\\.gov\\.hmrc\\.BuildInfo;.*\\.Routes;.*\\.RoutesPrefix;;Module;GraphiteStartUp;.*\\.Reverse[^.]*",
    ScoverageKeys.coverageMinimum := 63,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

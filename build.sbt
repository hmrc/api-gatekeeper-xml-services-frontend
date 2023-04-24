import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import bloop.integrations.sbt.BloopDefaults
import net.ground5hark.sbt.concat.Import._
import com.typesafe.sbt.uglify.Import._

val appName = "api-gatekeeper-xml-services-frontend"

scalaVersion := "2.13.8"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=false",
      "dead_code=true"
    ),
    uglify / includeFilter := GlobFilter("apis-*.js"),
    Assets / pipelineStages := Seq(
      concat,
      uglify
    ),
    majorVersion := 0,
    routesImport += "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.controllers.binders._",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.apigatekeeperxmlservicesfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .settings(publishingSettings, scoverageSettings)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings))
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := ";.*\\.domain\\.models\\..*;uk\\.gov\\.hmrc\\.BuildInfo;.*\\.Routes;.*\\.RoutesPrefix;;Module;GraphiteStartUp;.*\\.Reverse[^.]*",
    ScoverageKeys.coverageMinimumStmtTotal := 96,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

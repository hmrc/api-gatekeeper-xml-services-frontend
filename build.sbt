import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import bloop.integrations.sbt.BloopDefaults
import net.ground5hark.sbt.concat.Import.*
import com.typesafe.sbt.uglify.Import.*
import uk.gov.hmrc.DefaultBuildSettings

val appName = "api-gatekeeper-xml-services-frontend"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
ThisBuild / majorVersion := 0


lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
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
  .settings(ScoverageSettings())
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )


lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    name := "integration-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    headerSettings(Test) ++ automateHeaderSettings(Test)
  )


Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))


commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: state},
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: state},
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: state},
  Command.command("testAll") { state => "test" :: "it/test" :: state},
  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "run-all-tests" :: "coverageOff" :: "coverageAggregate" :: state }
)
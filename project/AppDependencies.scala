import sbt._

object AppDependencies {

  val bootstrapVersion     = "10.7.0"
  val apiDomainVersion     = "0.21.0"
  val playfrontendVersion  = "12.32.0"
  val mockitoScalaVersion  = "2.0.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"   % playfrontendVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "4.3.0",
    "org.typelevel"     %% "cats-core"                    % "2.13.0",
    "org.apache.commons" % "commons-csv"                  % "1.14.1",
    "commons-validator"  % "commons-validator"            % "1.10.1",
    "commons-io"         % "commons-io"                   % "2.21.0",
    "uk.gov.hmrc"       %% "api-platform-api-domain"      % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion,
    "org.jsoup"    % "jsoup"                   % "1.22.1"
  )
    .map(_ % "test")
}

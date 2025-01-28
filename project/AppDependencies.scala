import sbt._

object AppDependencies {

  lazy val bootstrapVersion  = "9.7.0"
  val apiDomainVersion       = "0.19.1"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"   % "11.11.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "3.0.0",
    "org.typelevel"     %% "cats-core"                    % "2.12.0",
    "org.apache.commons" % "commons-csv"                  % "1.12.0",
    "commons-validator"  % "commons-validator"            % "1.9.0",
    "commons-io"         % "commons-io"                   % "2.18.0",
    "uk.gov.hmrc"       %% "api-platform-api-domain"      % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.29",
    "org.jsoup"            % "jsoup"                   % "1.15.4"
  )
    .map(_ % "test")
}

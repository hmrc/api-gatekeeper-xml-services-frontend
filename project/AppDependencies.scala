import sbt._

object AppDependencies {

  lazy val bootstrapVersion  = "8.4.0"
  val apiDomainVersion       = "0.14.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"   % "8.5.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "1.10.0",
    "uk.gov.hmrc"       %% "emailaddress"                 % "3.8.0",
    "org.typelevel"     %% "cats-core"                    % "2.9.0",
    "org.apache.commons" % "commons-csv"                  % "1.10.0",
    "commons-io"         % "commons-io"                   % "2.11.0",
    "uk.gov.hmrc"       %% "api-platform-api-domain"      % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.29",
    "org.jsoup"            % "jsoup"                   % "1.15.4"
  )
    .map(_ % "test, it")
}

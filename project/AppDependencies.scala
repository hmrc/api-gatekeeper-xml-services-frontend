import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.6.2"
  lazy val bootstrapVersion = "7.12.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28"         % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"                 % "5.1.0-play-28",
    "uk.gov.hmrc"             %% "internal-auth-client-play-28"       % "1.2.0",
    "uk.gov.hmrc"             %% "emailaddress"                       % "3.7.0",
    "org.typelevel"           %% "cats-core"                          % "2.4.2",
    "org.apache.commons"      %  "commons-csv"                        % "1.8",
    "commons-io"              %  "commons-io"                         % "2.11.0",
    "com.beachape"            %% "enumeratum-play-json"               % enumeratumVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"             % bootstrapVersion,
    "org.mockito"             %% "mockito-scala-scalatest"            % "1.7.1",
    "org.jsoup"               %  "jsoup"                              % "1.13.1",
    "com.vladsch.flexmark"    %  "flexmark-all"                       % "0.36.8")
    .map(_ % "test, it")
}

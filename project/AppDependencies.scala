import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.6.2"
  lazy val bootstrapVersion = "5.24.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "1.26.0-play-28",
     "uk.gov.hmrc"            %% "auth-client"                % "5.8.0-play-28",
    "org.typelevel"           %% "cats-core"                  % "2.4.2",
    "uk.gov.hmrc"             %% "emailaddress"               % "3.5.0",
    "org.apache.commons"      % "commons-csv"                 % "1.8",
    "commons-io"              % "commons-io"                  % "2.11.0",
    "com.beachape"            %% "enumeratum-play-json"       % enumeratumVersion,
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion    % "test, it",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.7.1"             % "test, it",
    "org.jsoup"               %  "jsoup"                      % "1.13.1"            % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it"
  )
}

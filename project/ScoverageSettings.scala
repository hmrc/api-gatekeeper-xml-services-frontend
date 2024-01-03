import sbt.Keys.parallelExecution
import sbt.Test
import scoverage.ScoverageKeys

object ScoverageSettings {
  def apply() = Seq(
    ScoverageKeys.coverageExcludedPackages := Seq(
      "<empty>",
      """.*\.domain\.models\..*""",
      """uk\.gov\.hmrc\.BuildInfo""" ,
      """.*\.Routes""",
      """.*\.RoutesPrefix""",
      """Module""",
      """GraphiteStartUp""",
      """.*\.Reverse[^.]*"""
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 96,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

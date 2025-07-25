resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"        % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"    % "2.6.0")
addSbtPlugin("org.playframework"  % "sbt-plugin"            % "3.0.7")
addSbtPlugin("com.github.sbt"     % "sbt-gzip"              % "2.0.0")
addSbtPlugin("io.github.irundaia" % "sbt-sassify"           % "1.5.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"         % "2.3.1")
addSbtPlugin("org.scalastyle"     % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"          % "2.5.2")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"          % "0.14.3")
addSbtPlugin("ch.epfl.scala"      % "sbt-bloop"             % "2.0.6")
addSbtPlugin("com.github.sbt"     % "sbt-concat"            % "1.0.0")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

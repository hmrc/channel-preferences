resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.iheart"        % "sbt-play-swagger"      % "0.10.2")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"          % "1.16")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.5.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"            % "2.8.11")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.9.2")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "3.0.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-bobby"             % "3.2.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.1.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-service-manager"   % "0.8.0")
addSbtPlugin("com.iheart"        % "sbt-play-swagger"      % "0.10.2")

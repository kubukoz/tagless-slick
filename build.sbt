val commonSettings = Seq(
  scalaVersion := "2.12.7",
  scalacOptions ++= Options.all,
  fork in Test := true,
  libraryDependencies ++= Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
    "com.typesafe.slick"   %% "slick"                % "3.2.3",
    "co.fs2"               %% "fs2-reactive-streams" % "1.0.0-RC1",
    "com.github.mpilquist" %% "simulacrum"           % "0.13.0",
    "org.postgresql"       % "postgresql"            % "42.2.4",
    "com.github.gvolpe"    %% "console4cats"         % "0.3" % Test,
    "org.scalatest"        %% "scalatest"            % "3.0.4" % Test
  )
)

val core = project.settings(commonSettings)

val taglessSlick = project.in(file(".")).settings(commonSettings).dependsOn(core).aggregate(core)

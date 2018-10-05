inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/tagless-slick")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  ))

val commonSettings = Seq(
  scalaVersion := "2.12.7",
  scalacOptions ++= Options.all,
  fork in Test := true,
  name := "tagless-slick",
  updateOptions := updateOptions.value.withGigahorse(false), //may fix publishing bug
  libraryDependencies ++= Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
    "com.typesafe.slick"   %% "slick"                % "3.2.3",
    "co.fs2"               %% "fs2-reactive-streams" % "1.0.0",
    "com.github.mpilquist" %% "simulacrum"           % "0.13.0",
    "org.postgresql"       % "postgresql"            % "42.2.4" % Test,
    "com.github.gvolpe"    %% "console4cats"         % "0.3" % Test,
    "org.scalatest"        %% "scalatest"            % "3.0.4" % Test
  )
)

val core = project.settings(commonSettings).settings(name += "-core")

val taglessSlick =
  project.in(file(".")).settings(commonSettings).settings(skip in publish := true).dependsOn(core).aggregate(core)

enablePlugins(ScalaJSPlugin)

name := "akka.js_site"

organization := "eu.unicredit"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-feature", "-language:_", "-deprecation")

//resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "eu.unicredit" %%% "akkajsactorstream" % "0.1.2-SNAPSHOT",
  "eu.unicredit" %%% "akkajsactor" % "0.1.2-SNAPSHOT",
  "com.lihaoyi" %%% "scalatags" % "0.5.4"
)

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage

scalaJSUseRhino in Global := false

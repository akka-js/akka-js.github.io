enablePlugins(ScalaJSPlugin)

name := "akka.js_site"

organization := "eu.unicredit"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-feature", "-language:_", "-deprecation")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "eu.unicredit" %%% "akkajsactorstream" % "0.2.0",
  "eu.unicredit" %%% "akkajsactor" % "0.2.0",
  "com.lihaoyi" %%% "scalatags" % "0.6.0"
)

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage

scalaJSUseRhino in Global := false

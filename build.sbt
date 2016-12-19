enablePlugins(ScalaJSPlugin)

name := "akka.js_site"

organization := "eu.unicredit"

scalaVersion  := "2.12.0"

scalacOptions := Seq("-feature", "-language:_", "-deprecation")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.akka-js" %%% "akkajsactorstream" % "0.2.4.16",
  "org.akka-js" %%% "akkajsactor" % "0.2.4.16",
  "com.lihaoyi" %%% "scalatags" % "0.6.2"
)

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage

scalaJSUseRhino in Global := false

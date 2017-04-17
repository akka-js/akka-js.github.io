enablePlugins(ScalaJSPlugin)

name := "akka.js_site"

organization := "eu.unicredit"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-feature", "-language:_", "-deprecation")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.akka-js" %%% "akkajsactorstream" % "1.2.5.0",
  "org.akka-js" %%% "akkajsactor" % "1.2.5.0",
  "com.lihaoyi" %%% "scalatags" % "0.6.3"
)

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage

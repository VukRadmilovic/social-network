name := """praksa-vuk-radmilovic-backend"""
organization := "com.novalite"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies += "com.github.jwt-scala" %% "jwt-play-json" % "9.4.3"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.33"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.1.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.novalite.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.novalite.binders._"

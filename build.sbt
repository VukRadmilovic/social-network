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
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
libraryDependencies += "io.minio" % "minio" % "8.0.0"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "software.amazon.awssdk" % "s3" % "2.20.65"
libraryDependencies += "software.amazon.awssdk" % "core" % "2.20.65"
libraryDependencies += "software.amazon.awssdk" % "auth" % "2.20.65"
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.novalite.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.novalite.binders._"

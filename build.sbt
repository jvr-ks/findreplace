name := "findreplace"

version := "1.0"

// Für JAVA 8!
//crossScalaVersions := Seq("2.11.7", "2.10.3")

scalaVersion := "2.11.12"

resolvers += "Maven" at "https://repo1.maven.org/maven2/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

//libraryDependencies ++= Seq(
	//"log4j" % "log4j" % "1.2.14",
	//"org.scala-lang" % "scala-swing" % "2.11.0-M7",
	//"org.scala-lang" % "scala-actors" % "2.11.0",
	//"org.scala-lang" % "scala-library" % "2.11.0",
	//"org.scala-lang.modules" %% "scala-xml" % "1.0.1"
//)


//NEU:
// https://mvnrepository.com/artifact/org.scala-lang.modules/scala-swing
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"

// https://mvnrepository.com/artifact/org.scala-lang/scala-actors
libraryDependencies += "org.scala-lang" % "scala-actors" % "2.11.12"

// https://mvnrepository.com/artifact/org.scala-lang.modules/scala-xml
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

// https://mvnrepository.com/artifact/net.java.dev.jna/jna
libraryDependencies += "net.java.dev.jna" % "jna" % "5.12.1"

// https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform
libraryDependencies += "net.java.dev.jna" % "jna-platform" % "5.12.1"

// https://github.com/Log4s/log4s
libraryDependencies += "org.log4s" %% "log4s" % "1.8.2"

// https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11" % Test

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.32"



scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")


Compile / unmanagedJars += file("lib_0/jfugue-4.0.3.jar")

run / fork := true


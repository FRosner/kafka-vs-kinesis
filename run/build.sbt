lazy val root = (project in file("."))
  .settings(
    organization in ThisBuild := "de.frosner",
    scalaVersion in ThisBuild := "2.12.6",
    version      in ThisBuild := "0.1.0-SNAPSHOT",
    name := "run",
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    libraryDependencies ++= List(
        "net.cakesolutions" %% "scala-kafka-client" % "1.1.0",
        "net.cakesolutions" %% "scala-kafka-client-akka" % "1.1.0"
    )
  )

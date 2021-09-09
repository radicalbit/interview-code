
lazy val `kafka-mirror-consumer-producer` = (project in file("."))
  .settings(
    name := "kafka-mirror-consumer-producer",
    version := "0.1",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Dependencies.KafkaMirrorConsumerProducer.libraries,
  )

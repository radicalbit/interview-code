import sbt._

object Dependencies {

  private object kafka {
    lazy val version   = "2.4.1"
    lazy val namespace = "org.apache.kafka"
    lazy val core      = namespace %% "kafka" % version
  }

  private object scalaTest {
    lazy val version      = "3.2.5"
    private val namespace = "org.scalatest"
    lazy val core         = namespace %% "scalatest" % version
  }

  private object embeddedKafka {
    private val version   = "2.4.1.1"
    private val namespace = "io.github.embeddedkafka"
    lazy val core         = namespace %% "embedded-kafka" % version
  }

  object mockito {
    lazy val version   = "1.16.37"
    lazy val namespace = "org.mockito"
    lazy val core      = namespace %% "mockito-scala" % version
  }

  object config {
    lazy val version   = "1.3.1"
    lazy val namespace = "com.typesafe"
    lazy val core      = namespace % "config" % version
  }

  object logback {
    lazy val version   = "1.1.3"
    lazy val namespace = "ch.qos.logback"
    lazy val core      = namespace % "logback-classic" % version
  }

  object KafkaMirrorConsumerProducer {

    lazy val overrideLibraries = Seq()

    lazy val libraries = Seq(
      kafka.core,
      scalaTest.core     % "test",
      embeddedKafka.core % "test",
      config.core,
      logback.core,
      mockito.core % "test"
    )
  }
}

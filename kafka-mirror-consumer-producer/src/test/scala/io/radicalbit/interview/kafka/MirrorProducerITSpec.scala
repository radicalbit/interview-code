package io.radicalbit.interview.kafka

import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.ConsumerExtensions.ConsumerOps
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionException}

class MirrorProducerITSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  implicit val decoder: ConsumerRecord[String, String] => (String, String) =
    cr => (cr.key(), cr.value)
  implicit val keyDeserializer: StringDeserializer = new StringDeserializer()

  private val embeddedKafkaConfig = EmbeddedKafkaConfig()

  val configString: String =
    s"""{"kafka": {
        "bootstrap": {"servers": "localhost:${embeddedKafkaConfig.kafkaPort}"},
        "producer": {"bootstrap": {"servers": "localhost:${embeddedKafkaConfig.kafkaPort}"}},
        "consumer": {"bootstrap": {"servers": "localhost:${embeddedKafkaConfig.kafkaPort}"}}
       }
      }"""

  val config = ConfigFactory.parseString(configString).withFallback(ConfigFactory.load())

  private val mirrorProducer = new MirrorProducer(config)

  "MirrorProducer" should {

    val key       = "I_Am_Key"
    val data      = """{"name": "Tuff"}"""
    val topicName = "topic_name"

    "properly write data on kafka" in {
      withRunningKafka {
        val _ =
          Await.result(mirrorProducer.send(key, data, topicName), Duration.Inf)

        val result =
          withConsumer[String, String, (String, String)] { consumer =>
            consumer.consumeLazily[(String, String)](topicName).take(1).head
          }._2

        result mustBe data
      }
    }
  }
}

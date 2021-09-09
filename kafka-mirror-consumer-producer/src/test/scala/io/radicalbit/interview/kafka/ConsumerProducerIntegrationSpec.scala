package io.radicalbit.interview.kafka

import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.ConsumerExtensions.ConsumerOps
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ConsumerProducerIntegrationSpec extends AnyWordSpec with Matchers with EmbeddedKafka with BeforeAndAfterEach {

  implicit val decoder: ConsumerRecord[String, String] => (String, String) = cr => (cr.key(), cr.value)
  implicit val deserializer: StringDeserializer                            = new StringDeserializer()
  implicit val stringSerializer: StringSerializer                          = new StringSerializer()

  private val startingData = "MessagePayload"

  implicit val embeddedKafkaConfigIngoing: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(kafkaPort = 6001, zooKeeperPort = 6000)

  val embeddedKafkaConfigOutgoing: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 6003, zooKeeperPort = 6002)

  val configString: String =
    s"""{"kafka": {
          "bootstrap": {"servers": "localhost:${embeddedKafkaConfigIngoing.kafkaPort}"},
          "producer": {"bootstrap": {"servers": "localhost:${embeddedKafkaConfigOutgoing.kafkaPort}"}},
          "consumer": {
            "bootstrap": {"servers": "localhost:${embeddedKafkaConfigIngoing.kafkaPort}"},
            "reading": {"limit": true},
            "values": {"to": {"read": 1}}
          }
        }
      }"""

  val config         = ConfigFactory.parseString(configString).withFallback(ConfigFactory.load())
  val mirrorProducer = new MirrorProducer(config)
  val mirrorConsumer = new MirrorConsumer(config, mirrorProducer)

  override def beforeEach(): Unit = {
    EmbeddedKafka.start()(embeddedKafkaConfigIngoing)

    EmbeddedKafka.start()(embeddedKafkaConfigOutgoing)
  }

  override def afterEach(): Unit = {
    EmbeddedKafka.stop()
  }

  "KafkaConsumerProducer" should {

    "consume avro data and produce json data" in {
      val inputTopic  = "input_topic"
      val outputTopic = "output_topic"

      // producer on first kafka instance
      EmbeddedKafka.withProducer[String, String, Unit](producer =>
        producer.send(new ProducerRecord[String, String](inputTopic, startingData)))

      // should read 1 message from first kafka instance and write on second
      mirrorConsumer.listen(inputTopic, outputTopic)

      // consumer on second kafka instance
      val resultSecondTopic =
        withConsumer[String, String, (String, String)] { consumer =>
          consumer.consumeLazily[(String, String)](outputTopic).take(1).head
        }(embeddedKafkaConfigOutgoing, deserializer, deserializer)._2

      resultSecondTopic mustBe startingData
    }

  }
}

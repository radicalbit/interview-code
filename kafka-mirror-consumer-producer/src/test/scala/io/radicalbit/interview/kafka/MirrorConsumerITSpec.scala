package io.radicalbit.interview.kafka

import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.ConsumerExtensions.ConsumerOps
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class MirrorConsumerITSpec extends AnyWordSpec with Matchers with EmbeddedKafka with BeforeAndAfterEach {

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

  "MirrorConsumer" should {

    "mirror data on a topic" in {
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

    "mirror data on multiple topics" in {
      val inputTopic     = "input_topic"
      val inputTopicTwo  = "input_topic_two"
      val outputTopic    = "output_topic"
      val outputTopicTwo = "output_topic_two"

      // producer on first kafka instance
      EmbeddedKafka.withProducer[String, String, Unit](producer => {
        producer.send(new ProducerRecord[String, String](inputTopic, startingData))
        producer.send(new ProducerRecord[String, String](inputTopicTwo, startingData))
      })

      // should read 1 message from first kafka instance and write on second
      Seq((inputTopic, outputTopic), (inputTopicTwo, outputTopicTwo)).foreach {
        case (source, output) =>
          mirrorConsumer.listen(source, output)
      }

      // consumer on second kafka instance
      withConsumer[String, String, Assertion] { consumer =>
        consumer
          .consumeLazily[(String, String)](outputTopic, outputTopicTwo)
          .take(2)
          .map { case (_, value) => value }
          .toList must contain theSameElementsAs Seq(startingData, startingData)
      }(embeddedKafkaConfigOutgoing, deserializer, deserializer)
    }

  }
}

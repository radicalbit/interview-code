package io.radicalbit.interview.kafka

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.util.Properties
import scala.concurrent.{ExecutionContext, Future}

class DebugJsonProducer(configuration: Config)(implicit ec: ExecutionContext) {

  private val log = LoggerFactory.getLogger(this.getClass)

  protected val producerProps: Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getString("kafka.producer.bootstrap.servers"))
    props.put(ProducerConfig.CLIENT_ID_CONFIG, configuration.getString("kafka.producer.client.id"))
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, configuration.getString("kafka.producer.max.block.ms"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props
  }

  val producer = {
    new KafkaProducer[String, String](producerProps)
  }

  def send(key: String, message: String, topic: String): Future[RecordMetadata] = {
    log.debug(s"Send message to Kafka on topic $topic")
    Future {
      producer.send(new ProducerRecord[String, String](topic, key, message)).get
    }
  }
}

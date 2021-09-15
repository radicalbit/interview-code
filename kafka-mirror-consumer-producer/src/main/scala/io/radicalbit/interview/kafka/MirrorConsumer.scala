package io.radicalbit.interview.kafka

import KafkaConstants.{defaultCountValue, defaultLimitValue}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeoutException
import java.util.stream.Collectors
import scala.concurrent.ExecutionContext

class MirrorConsumer(configuration: Config, producer: MirrorProducer)(implicit executionContext: ExecutionContext) {

  private val log = LoggerFactory.getLogger(this.getClass)

  var shouldRun: Boolean = true

  private val withReadingLimit     = configuration.getBoolean("kafka.consumer.reading.limit")
  private val valueToRead          = configuration.getInt("kafka.consumer.values.to.read")
  private val pollWindow: Duration = Duration.ofSeconds(configuration.getInt("kafka.consumer.pool.window.seconds"))

  private val props = new Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getString("kafka.consumer.bootstrap.servers"))
  props.put(ConsumerConfig.GROUP_ID_CONFIG, configuration.getString("kafka.consumer.group.id"))
  props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, configuration.getInt("kafka.consumer.session.timeout"))
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val consumer = new KafkaConsumer[String, String](props)

  def listen(sourceTopic: String, outputTopic: String): Unit = {
    shouldRun = true

    log.info("Start listening")

    try {
      Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

      consumer.assign(
        consumer
          .partitionsFor(sourceTopic)
          .stream
          .map(partitionInfo => new TopicPartition(partitionInfo.topic, partitionInfo.partition))
          .collect(Collectors.toSet()))

      consumer.seekToBeginning(consumer.assignment())

      while (shouldRun) {
        processIncomingData(outputTopic)
      }
    } catch {
      case timeOutEx: TimeoutException =>
        timeOutEx.printStackTrace()
        None
      case ex: Exception =>
        ex.printStackTrace()
        None
    }

    def close(): Unit = shouldRun = false
  }

  private def processIncomingData(producerTopics: String): Unit = {
    var counter = 0

    val records: ConsumerRecords[String, String] = consumer.poll(pollWindow)
    val it                                       = records.iterator()

    while (it.hasNext && (withReadingLimit && counter < valueToRead)) {
      val record: ConsumerRecord[String, String] = it.next()
      val receivedItem                           = record.value()

      log.info(s"Value got is [$receivedItem]")

      if (withReadingLimit)
        counter += 1

      for { result <- producer.send(record.key(), receivedItem, producerTopics) } yield result
    }

    if (withReadingLimit && counter >= valueToRead)
      shouldRun = false
  }
}

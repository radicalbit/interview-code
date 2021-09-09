package io.radicalbit.interview

import io.radicalbit.interview.kafka.{MirrorConsumer, MirrorProducer}
import io.radicalbit.interview.service.DebugServiceRunner
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global

object Boot extends App {

  def startApplication() = {

    val config = ConfigFactory.load()

    val jsonProducer = new MirrorProducer(config)
    val avroConsumer = new MirrorConsumer(config, jsonProducer)

    val topicToMirror = Seq(("sourceOne", "outputOne"), ("sourceTwo", "outputTwo"), ("sourceThree", "outputThree"))

    new DebugServiceRunner(avroConsumer).startDebugService(topicToMirror)

    avroConsumer.consumer.close()
    jsonProducer.producer.close()
  }

  startApplication()

}

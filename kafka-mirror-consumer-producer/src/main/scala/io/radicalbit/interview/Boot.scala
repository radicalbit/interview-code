package io.radicalbit.interview

import io.radicalbit.interview.kafka.{MirrorConsumer, MirrorProducer}
import com.typesafe.config.ConfigFactory
import io.radicalbit.interview.service.MirrorServiceRunner

import scala.concurrent.ExecutionContext.Implicits.global

object Boot extends App {

  def startApplication() = {

    val config = ConfigFactory.load()

    val jsonProducer = new MirrorProducer(config)
    val avroConsumer = new MirrorConsumer(config, jsonProducer)

    val topicToMirror = Seq(("sourceOne", "outputOne"), ("sourceTwo", "outputTwo"), ("sourceThree", "outputThree"))

    new MirrorServiceRunner(avroConsumer).startMirrorService(topicToMirror)

    avroConsumer.consumer.close()
    jsonProducer.producer.close()
  }

  startApplication()

}

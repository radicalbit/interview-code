package io.radicalbit.interview.service

import io.radicalbit.interview.kafka.DebugAvroConsumer

class DebugServiceRunner(debugAvroConsumer: DebugAvroConsumer) extends DebugService {

  @throws[IllegalArgumentException]
  def startDebugService(topicPairs: Seq[(String, String)]): Unit = {
    log.info("Starting topic mirroring")
    topicPairs.foreach {
      case (sourceTopic, outputTopic) =>
        debugAvroConsumer.listen(sourceTopic, outputTopic)
    }
  }
}

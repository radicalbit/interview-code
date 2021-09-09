package io.radicalbit.interview.service

import io.radicalbit.interview.kafka.MirrorConsumer
import org.slf4j.LoggerFactory

class DebugServiceRunner(mirrorConsumer: MirrorConsumer) {
  protected val log = LoggerFactory.getLogger(this.getClass)

  @throws[IllegalArgumentException]
  def startDebugService(topicPairs: Seq[(String, String)]): Unit = {
    log.info("Starting topic mirroring")
    topicPairs.foreach {
      case (sourceTopic, outputTopic) =>
        mirrorConsumer.listen(sourceTopic, outputTopic)
    }
  }
}

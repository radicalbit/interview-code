package io.radicalbit.interview.service

import io.radicalbit.interview.kafka.MirrorConsumer
import org.slf4j.LoggerFactory

class MirrorServiceRunner(mirrorConsumer: MirrorConsumer) {
  protected val log = LoggerFactory.getLogger(this.getClass)

  @throws[IllegalArgumentException]
  def startMirrorService(topicPairs: Seq[(String, String)]): Unit = {
    log.info("Starting topic mirroring")
    topicPairs.foreach {
      case (sourceTopic, outputTopic) =>
        mirrorConsumer.listen(sourceTopic, outputTopic)
    }
  }
}

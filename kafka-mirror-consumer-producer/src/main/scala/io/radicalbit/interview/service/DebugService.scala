package io.radicalbit.interview.service

import org.slf4j.LoggerFactory

trait DebugService {

  protected val log = LoggerFactory.getLogger(this.getClass)
}

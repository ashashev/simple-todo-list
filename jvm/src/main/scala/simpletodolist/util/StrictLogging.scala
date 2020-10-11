package simpletodolist.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait StrictLogging {
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)
}

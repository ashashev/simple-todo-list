package simpletodolist.server

import org.apache.commons.daemon._

import simpletodolist.util.StrictLogging

class Service extends Daemon with StrictLogging {

  def init(daemonContext: DaemonContext): Unit = {
    init(daemonContext.getArguments)
  }

  def init(args: Array[String]): Unit = {
    logger.debug(s"init, args: [${args.mkString(", ")}]")
  }

  def start(): Unit = {
    logger.debug("start")
  }

  def stop(): Unit = {
    logger.debug("stop")
  }

  def destroy(): Unit = {
    logger.debug("destroy")
  }
}

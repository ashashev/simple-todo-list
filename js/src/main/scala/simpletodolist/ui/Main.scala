package simpletodolist.ui

import scala.language.implicitConversions

object Main {
  val control = Control(Config(Globals.config.toMap))

  def main(args: Array[String]): Unit = {
    control.init()
  }

}

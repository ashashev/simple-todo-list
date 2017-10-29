package simpletodolist.ui

object Config {
  def apply(data: Map[String, String]): Config = new Config(
    data.getOrElse("wsUrl", "ws://localhost"),
    Mode(data.get("mode"))
  )
}

trait Mode

object Mode {
  def apply(mode: Option[String]): Mode = mode match {
    case Some("view") => View
    case Some("edit") => Edit
    case _ => Unknown
  }

  case object View extends Mode

  case object Edit extends Mode

  case object Unknown extends Mode

}

case class Config(wsUrl: String, mode: Mode)


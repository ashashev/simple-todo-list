package simpletodolist.ui

import simpletodolist.library._

object Config {
  import org.scalajs.dom.{document, Location}

  private implicit class DataExtractor(data: Map[String, String]) {
    private def getId(location: Location) = {
      queryStringToMap(location.search).getOrElse("id", "")
    }

    def url: String =
      data.getOrElse("url", "ws://localhost")

    def mode: Mode =
      Mode(data.get("mode"))

    def id: String = {
      val id = data.getOrElse("id", "")
      if (id.nonEmpty) id
      else getId(document.location)
    }
  }

  def apply(data: Map[String, String]): Config = new Config(
    data.url,
    data.mode,
    data.id
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

case class Config(url: String, mode: Mode, id: String)


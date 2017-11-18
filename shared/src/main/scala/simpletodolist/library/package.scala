package simpletodolist

package object library {
  def queryStringToMap(query: String): Map[String, String] = {
    query.split(Array('?','&')).
      filterNot(s => s.isEmpty || s.startsWith("=")).
      map { s =>
        val ind = s.indexOf('=')
        if (ind == -1) (s -> "")
        else {
          val (key, value) = s.splitAt(ind)
          (key -> value.drop(1))
        }
      }.
      toMap
  }
}

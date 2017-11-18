package simpletodolist.library

import org.scalatest.FunSuite

class QueryStringToMapTest extends FunSuite {
  test("Simple case") {
    val query = "?val1=value1&val2=value2&val3=value3"
    val expected = Map("val1" -> "value1", "val2" -> "value2", "val3" -> "value3")
    val got = queryStringToMap(query)
    assert(got === expected)
  }

  test("Value has equal sign") {
    val query = "?val1=value1&val2=qq=ww&val3=value3"
    val expected = Map("val1" -> "value1", "val2" -> "qq=ww", "val3" -> "value3")
    val got = queryStringToMap(query)
    assert(got === expected)
  }

  test("Empty value without equal sign after name") {
    val query = "?val1=value1&val2&val3=value3"
    val expected = Map("val1" -> "value1", "val2" -> "", "val3" -> "value3")
    val got = queryStringToMap(query)
    assert(got === expected)
  }

  test("Empty value with equal sign after name") {
    val query = "?val1=value1&val2=&val3=value3"
    val expected = Map("val1" -> "value1", "val2" -> "", "val3" -> "value3")
    val got = queryStringToMap(query)
    assert(got === expected)
  }
}


package simpletodolist.library

import org.scalatest.FlatSpec

class ItemSpec extends FlatSpec {
  "The method createId" should "create different IDs" in {
    val id1 = Item.createId()
    val id2 = Item.createId()
    assert(id1 !== id2)
  }

  "The method toRaw" should "create right string from checked Item" in {
    val sid = "fbfac2eb-9034-4781-a69c-c147173fdf40"
    val item = Item(Item.idFromString(sid), true, "some item")
    val expected = s"""$sid;+;some item"""
    val got = item.toRaw
    assert(got === expected)
  }

  it should "create right string from unchecked Item" in {
    val sid = "3c94e34e-e286-11e7-80c1-9a214cf093ae"
    val item = Item(Item.idFromString(sid), false, "another item")
    val expected = s"""$sid;-;another item"""
    val got = item.toRaw
    assert(got === expected)
  }

  "The string is got by toRaw method from checked Item" should "produce the same Item" in {
    val item = Item(Item.createId(), true, "test")
    val raw = item.toRaw
    val got = Item(raw)
    assert(got === item)
  }

  "The string is got by toRaw method from unchecked Item" should "produce the same Item" in {
    val item = Item(Item.createId(), false, "another test")
    val raw = item.toRaw
    val got = Item(raw)
    assert(got === item)
  }

  "The string 'first item'" should "produce right unchecked Item" in {
    val str = "first item"
    val got = Item(str)
    assert(!got.checked)
    assert(got.text === str)
  }

  "The string '+ second item'" should "produce right checked Item" in {
    val str = "+ second item"
    val got = Item(str)
    assert(got.checked)
    assert(got.text === "second item")
  }

  "The string '  item around with space  '" should "produce right unchecked Item" in {
    val str = "  item around with space  "
    val got = Item(str)
    assert(!got.checked)
    assert(got.text === str.trim)
  }

  "The string 'fbfac2eb-9034-4781-a69c-c147173fdf40;text'" should "produce AssertionError" in {
    assertThrows[AssertionError] {
      val str = "fbfac2eb-9034-4781-a69c-c147173fdf40;text"
      Item(str)
    }
  }

  "The string 'fbfac2eb-9034-4781-a69c-c147173fdf40;+;text;more'" should "produce AssertionError" in {
    assertThrows[AssertionError] {
      val str = "fbfac2eb-9034-4781-a69c-c147173fdf40;+;text;more"
      Item(str)
    }
  }
}

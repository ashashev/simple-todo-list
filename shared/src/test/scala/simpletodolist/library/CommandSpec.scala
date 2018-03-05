package simpletodolist.library

import org.scalatest.FlatSpec

class CommandSpec extends FlatSpec {
  val items = List(
    Item(Item.createId(), true, "checked item"),
    Item(Item.createId(), false, "unchecked item"),
    Item(Item.createId(), false, "one more item")
  )

  "The Get command" should "correctly serialize to the string when instructed to" in {
    val got = Get.toRaw
    assert(got === "@GET@\n")
  }

  "The Update command" should "correctly serialize to the string when instructed to" in {
    val cmd = Update(items.head)
    val expected = "@UPD@\n" + items.head.toRaw
    val got = cmd.toRaw
    assert(got === expected)
  }

  "The Replace command" should "correctly serialize to the string when instructed to" in {
    val cmd = Replace(items)
    val expected = "@REP@\n" + items.map(_.toRaw).mkString("\n")
    val got = cmd.toRaw
    assert(got === expected)
  }

  "The Command.apply" can "create the command Get from string without '\\n' of the end" in {
    val got = Command("@GET@")
    assert(got === Get)
  }

  it can "create the command Get from string with '\\n' of the end" in {
    val got = Command("@GET@\n")
    assert(got === Get)
  }

  it can "create the command Update from string without '\\n' of the end" in {
    val input = "@UPD@\n" + items.head.toRaw
    val expected = Update(items.head)
    val got = Command(input)
    assert(got === expected)
  }

  it can "create the command Update from string with '\\n' of the end" in {
    val input = "@UPD@\n" + items.head.toRaw + "\n"
    val expected = Update(items.head)
    val got = Command(input)
    assert(got === expected)
  }

  it can "create the command Replace from string without '\\n' of the end" in {
    val input = "@REP@\n" + items.map(_.toRaw).mkString("\n")
    val expected = Replace(items)
    val got = Command(input)
    assert(got === expected)
  }

  it can "create the command Replace from string with '\\n' of the end" in {
    val input = "@REP@\n" + items.map(_.toRaw).mkString("\n") + "\n"
    val expected = Replace(items)
    val got = Command(input)
    assert(got === expected)
  }

  it should "throw exception if unknown command" in {
    val input = "SOMECMD\n" + items.map(_.toRaw).mkString("\n") + "\n"
    assertThrows[Error] {
      Command(input)
    }

    try {
      Command(input)
    } catch {
      case e: Error =>
        assert(e.getMessage.contains(input), "Error message contains input string")
    }
  }

  "Item.makeList" should "create empty list from empty string" in {
    val got = Item.makeList("")
    assert(got === List.empty[Item])
  }

  it should "skip empty strings and strings equal '+'" in {
    val got = Item.makeList(
      """
        |item1
        |
        |+
        | +
        |item2
      """.stripMargin)
    assert(got.size === 2)
    assert(got(0).text === "item1")
    assert(got(1).text === "item2")
  }
}

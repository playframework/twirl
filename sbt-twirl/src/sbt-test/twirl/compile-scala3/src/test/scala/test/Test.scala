package test

import play.twirl.api.*

object Test extends App {
  def test(template: HtmlFormat.Appendable, expected: String) = {
    assert(template.body == expected, s"Found '$template' but expected '$expected'")
  }

  test(a.b.html.c.render("world"), "Hello, world.\n")

  test(html.template.render("42"), "Answer: 42\n")

  test(
    html.manyargstemplate.render(
      "1", "2", "3", "5", "4", "7", "6", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26"),
    "26 args: 1 2 3 5 4 7 6 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26\n"
  )
}

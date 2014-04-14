package test

import twirl.api._

object Test extends App {
  def test(template: HtmlFormat.Appendable, expected: String) = {
    assert(template.body == expected, "Found '" + template + "' but expected '" + expected + "'")
  }

  test(a.b.html.c.render("world"), "\nHello, world.\n")

  test(html.template.render("42"), "\nAnswer: 42\n")
}

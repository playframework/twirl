import play.twirl.api._

object Test extends App {
  def test(template: HtmlFormat.Appendable, expected: String) = {
    assert(template.body == expected, s"Found '$template' but expected '$expected'")
  }

  test(a.b.html.c.render("world"), "Hello, world.\n")

  test(html.template.render("42"), "Answer: 42\n")

  test(html.xml_test.render(<a>foo</a>), "hello xml: <a>foo</a>\n")
}

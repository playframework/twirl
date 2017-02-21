# Dependency Injection with Templates

Twirl templates can be generated as a class rather than a static object by declaring a constructor using a special @this(args) syntax at the top of the template. This means that Twirl templates can be injected into controllers directly and can manage their own dependencies, rather than the controller having to manage dependencies not only for itself, but also for the templates it has to render.

As an example, suppose a template has a dependency on a component `Summarizer`, which is not used by the controller:

```java
public interface Summarizer {
  /** Provide short form of string if over a certain length */
  String summarize(String item);
}
```

Create a file `app/views/IndexTemplate.scala.html` using the `@this` syntax for the constructor:

```scala
@this(summarizer: Summarizer)
@(item: String)

@{summarizer.summarize(item)}
```

And finally define the controller in Play by injecting the template in the constructor:

```java
public class MyController extends Controller {
  
  private final views.html.IndexTemplate template;

  @Inject
  public MyController(views.html.IndexTemplate template) {
    this.template = template;
  }

  public Result index() {
    String item = "some extremely long text";
    return ok(template.render(item));
  }

}
```

Once the template is defined with its dependencies, then the controller can have the template injected into the controller, but the controller does not see `Summarizer`.

If you are using Twirl outside of a Play application, you will have to manually add the `@Inject` annotation saying that dependency injection should be used here:

```scala
TwirlKeys.constructorAnnotations += "@javax.inject.Inject()"
```

Inside a play application, this is already included in the default settings.
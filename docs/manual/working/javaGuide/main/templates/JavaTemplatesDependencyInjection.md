# Dependency Injection with Templates

Twirl templates can now be created with a constructor annotation using `@this`.  The constructor annotation means that Twirl templates can be injected into templates directly and can manage their own dependencies, rather than the controller having to manage dependencies not only for itself, but also for the templates it has to render.

As an example, suppose a template has a dependency on a component `TemplateRenderingComponent`, which is not used by the controller.  

First, add the `@Inject` annotation to Twirl in `build.sbt`:

```scala
TwirlKeys.constructorAnnotations += "@javax.inject.Inject()"
```

Then create a file `indexTemplate.scala.html` using the `@this` syntax for the constructor:

```scala
@this(trc: TemplateRenderingComponent)
@()

@{trc.render(item)}
```

And finally define the controller in Play by injecting the template in the constructor:

```java
public class MyController extends Controller {
  
  private final views.html.indexTemplate template;

  @Inject
  public MyController(views.html.indexTemplate template) {
    this.template = template;
  }

  public Result index() {
    return ok(template.render());
  }

}
```

Once the template is defined with its dependencies, then the controller can have the template injected into the controller, but the controller does not see `TemplateRenderingComponent`.
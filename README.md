# IES Aggregator Testkit

A scenario harness for aggregators built on the [IES Aggregator API](https://github.com/sitepark/ies-aggregator).
It loads a JSON scenario as an in-memory CMS, wires the **real** assembler chain around it through
the ports of the API, runs an aggregation and compares the resulting resource with a golden file.

The production classes run unmodified. What the harness replaces are the ports the platform would
supply — the repository, the channel, the image scaler, the variant catalog — and it does so in a
way that is deliberately **not more generous** than production: an aggregation error fails the test
instead of being logged, the clock is frozen, the scaler is deterministic.

```mermaid
flowchart LR
    S[("scenario.json<br/>repository, options, …")]
    R["Repository<br/><i>in-memory CMS</i>"]
    M["ScenarioModule<br/><i>binds the ports</i>"]
    G["Guice injector<br/><i>builds assemblers and<br/>collaborators from @Inject</i>"]
    A["your aggregator<br/><i>unmodified</i>"]
    O[("resource as<br/>normalized JSON")]
    E[("scenario.expected.json")]
    S --> R --> M --> G --> A --> O
    O <-->|assertThat| E
```

## Using it in a project

```xml
<dependency>
  <groupId>com.sitepark.ies</groupId>
  <artifactId>ies-aggregator-testkit</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

The base class below is JUnit and AssertJ; the harness declares both as `provided`, so a project
brings `junit-jupiter-params` and `assertj-core` along on its own terms — as it does anyway to write
tests at all.

The harness does not know what your resource looks like — which area holds the content, how the
root of the component tree is marked, which container a section is aggregated into. You tell it
once, in a base class of your own, ideally built from the constants of your resource model so there
is nothing to keep in step:

```java
abstract class SectionTypeScenarioTest<A extends SectionTypeAggregator> extends ScenarioTest {

  private final Class<A> type;
  private final String componentType;

  protected SectionTypeScenarioTest(Class<A> type, String componentType, String... directories) {
    super(directories);
    this.type = type;
    this.componentType = componentType;
  }

  @Override
  protected ScenarioLayout layout() {
    return new ScenarioLayout(Resource.CONTENT, Resource.CONTENT_ROOT, "main", "main");
  }

  @Override
  protected String aggregate(ScenarioContext context) {
    return context.aggregate(
        context.aggregator(this.type)::aggregateSectionType, this.componentType);
  }
}
```

One such class per kind of aggregation a project has — a section type, an area of the resource, the
meta file of an embedded medium. A suite is then what it is about, and nothing else:

```java
class TextAggregatorTest extends SectionTypeScenarioTest<TextSectionAggregator> {

  TextAggregatorTest() {
    super(TextSectionAggregator.class, "text", "scenarios/text");
  }
}
```

Every `<name>.json` under `scenarios/text` runs as its own parameterized case and its output is
compared with `<name>.expected.json`. A new case is two files, no code. Several directories may feed
one suite; they are read in the order given.

A suite may add tests of its own beside the inherited one — it is an ordinary test class, and
`load(resource)` opens a single scenario for them. Where the base class does not fit, its pieces are
still there on their own: `Scenarios.discover(dirs)`, `ScenarioContext.load(resource, layout)`,
`context.aggregate(…)`.

`context.aggregator(Class)` builds the aggregator under test the way the production container does —
from its `@Inject` constructor, with the harness's ports injected, and, if it is `OptionsAware`, with
the options of the scenario set. That is also how a project reaches the aggregators of a library it
extends: their constructors are package-private, as production never calls them either.

## A scenario file

```json
{
  "options": { "showDate": true },
  "access": { "mode": "ALLOW", "groups": ["editors"] },
  "objectType": { "homePage": false },
  "variantConfig": { "teaser": { "fitMode": "cover", "aspectRatio": "3x2", "formats": [ {"type": "webp"} ] } },
  "repository": {
    "1000": { "id": "1000", "objectType": "page", "content": { "sp_title": "Hello" } },
    "2000": { "id": "2000", "objectType": "media", "media": { "…": "…" } }
  }
}
```

| Block | Stands in for |
|---|---|
| `repository` | the CMS: a flat map `id → entry`, fields under `content`, links as id references, groups by a `group` block, media by a `media` block, anchors indexed |
| `options` | the aggregator's configured options, bound with `context.options(Class)` |
| `access` | the access restriction the channel reports for the resource |
| `objectType` | the configuration of the resource's object type |
| `variantConfig` | the image-variant catalog of the project configuration |

The entry `1000` is the source the aggregation starts from (`ScenarioContext.SOURCE_ID`).

## What the harness stands in for

| Port | Double |
|---|---|
| `RootResolverFactory`, resolvers | `Repository`, `RepositoryResolver` — navigation across links, groups, uploads |
| `MediaProvider` | `RepositoryMediaProvider` — from the entry's `media` block |
| `ChannelProvider`, `Channel` | `ScenarioChannel` — deterministic URLs, answers only what a publication would |
| `ImageScaler` | `ScenarioImageScaler` — sizes and URLs derived from the request, no pixels |
| `ObjectTypeConfigProvider`, `VariantConfigProvider`, `StructuredValueParser` | read from the scenario blocks |
| `AssemblerFactory` | `ScenarioAssemblerFactory` — ClassGraph finds `@AssemblerBinding` classes, the production lookup semantics pick the winner, Guice builds it |
| `AggregatorErrorHandler` | throws — a scenario is a fixture, an error is a failure |
| `Clock` | fixed at `2026-09-01T12:00:00Z` |

Everything concrete — services, resolvers, builders, dispatchers — is reached through just-in-time
bindings. An assembler with a collaborator of its own needs no change to the harness.

Two aggregation paths are offered, and they differ on purpose. `aggregate(ComponentAggregation, type)`
asks the component aggregation directly and attaches the component if it reports content.
`aggregateAsSection(Aggregator, type)` drives the aggregator the way the production tag does and
decides by what was written, not by what was reported — the only method that answers whether a
section reaches the page the way production answers it.

## On coverage

The harness is exercised by the scenario suites of the projects that use it, not by its own tests.
The tests here cover the discovery and lookup semantics of the assembler factory, the repository
doubles, and — through a probe suite that uses the harness the way a project does — the base class
and the options it sets. The coverage floor in `pom.xml` is the measured state of those tests, and
it is meant to be raised, not lowered.

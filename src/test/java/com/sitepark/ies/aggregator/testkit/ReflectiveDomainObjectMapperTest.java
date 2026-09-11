package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.output.OutputKeepIfEmpty;
import com.sitepark.ies.aggregator.output.OutputObject;
import com.sitepark.ies.aggregator.output.OutputProperty;
import com.sitepark.ies.aggregator.output.OutputUnwrapped;
import com.sitepark.ies.aggregator.output.format.JsonWriter;
import com.sitepark.ies.aggregator.value.text.PlainText;
import com.sitepark.ies.aggregator.value.text.Text;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavior tests for {@link ReflectiveDomainObjectMapper}, verifying it reproduces the relevant
 * {@code JacksonDomainObjectMapper} semantics through an actual {@link JsonWriter} serialization.
 */
class ReflectiveDomainObjectMapperTest {

  /** Serializes a single value the way the golden-file comparison does, and returns the JSON. */
  private static String toJson(Object value) {
    OutputObject root = new OutputObject(null, null);
    root.put("x", value);
    StringWriter writer = new StringWriter();
    root.accept(new JsonWriter(writer, new ReflectiveDomainObjectMapper()));
    return writer.toString();
  }

  record KeepSample(@OutputKeepIfEmpty Text keptEmpty, Text droppedEmpty, Text present) {}

  @Test
  void keepsEmptyPropertyMarkedKeepIfEmptyWhilePlainEmptySiblingIsDropped() {
    String json = toJson(new KeepSample(Text.empty(), Text.empty(), PlainText.of("here")));

    assertThat(json)
        .as("@OutputKeepIfEmpty property survives, plain empty sibling is dropped")
        .contains("\"keptEmpty\":\"\"")
        .contains("\"present\":\"here\"")
        .doesNotContain("droppedEmpty");
  }

  @OutputKeepIfEmpty
  record TypeKeptSample(Text a, Text b) {}

  @Test
  void keepsAllEmptyPropertiesWhenContainerTypeIsKeepIfEmpty() {
    String json = toJson(new TypeKeptSample(Text.empty(), Text.empty()));

    assertThat(json)
        .as("all empty properties of a @OutputKeepIfEmpty type are kept")
        .contains("\"a\":\"\"")
        .contains("\"b\":\"\"");
  }

  record RenameSample(@OutputProperty("renamed") Text label) {}

  @Test
  void renamesRecordPropertyViaOutputProperty() {
    String json = toJson(new RenameSample(PlainText.of("v")));

    assertThat(json)
        .as("@OutputProperty renames the record component's output key")
        .contains("\"renamed\":\"v\"")
        .doesNotContain("label");
  }

  record Inner(Text a) {}

  record OuterRecord(Text top, @OutputUnwrapped Object extension) {}

  @Test
  void inlinesNestedRecordViaOutputUnwrapped() {
    String json = toJson(new OuterRecord(PlainText.of("t"), new Inner(PlainText.of("i"))));

    assertThat(json)
        .as("@OutputUnwrapped inlines the nested record's properties flat")
        .contains("\"top\":\"t\"")
        .contains("\"a\":\"i\"")
        .doesNotContain("extension");
  }

  @Test
  void nullUnwrappedPropertyContributesNothing() {
    String json = toJson(new OuterRecord(PlainText.of("t"), null));

    assertThat(json)
        .as("a null @OutputUnwrapped property leaves no dangling key")
        .isEqualTo("{\"x\":{\"top\":\"t\"}}");
  }

  @Test
  void mergesMapValueOfOutputUnwrappedProperty() {
    String json = toJson(new OuterRecord(PlainText.of("t"), Map.of("k", PlainText.of("v"))));

    assertThat(json)
        .as("@OutputUnwrapped merges a Map value's entries as siblings")
        .contains("\"top\":\"t\"")
        .contains("\"k\":\"v\"");
  }

  record ExtA(Text e1) {}

  record ExtB(Text e2) {}

  record NestingExt(Text n, @OutputUnwrapped List<Object> extensions) {}

  record ListOuter(Text top, @OutputUnwrapped List<Object> extensions) {}

  @Test
  void inlinesEveryExtensionOfAListFlat() {
    String json =
        toJson(
            new ListOuter(
                PlainText.of("t"),
                List.of(new ExtA(PlainText.of("a")), new ExtB(PlainText.of("b")))));

    assertThat(json)
        .as("@OutputUnwrapped rolls out every list element's properties flat as siblings")
        .contains("\"top\":\"t\"")
        .contains("\"e1\":\"a\"")
        .contains("\"e2\":\"b\"");
  }

  @Test
  void lastExtensionWinsOnKeyCollision() {
    String json =
        toJson(
            new ListOuter(
                PlainText.of("t"),
                List.of(new ExtA(PlainText.of("first")), new ExtA(PlainText.of("last")))));

    assertThat(json)
        .as("on a colliding key the extension added last wins")
        .contains("\"e1\":\"last\"")
        .doesNotContain("first");
  }

  @Test
  void inlinesNestedExtensionListFlat() {
    String json =
        toJson(
            new ListOuter(
                PlainText.of("t"),
                List.of(new NestingExt(PlainText.of("x"), List.of(new ExtB(PlainText.of("z")))))));

    assertThat(json)
        .as("an extension carrying its own @OutputUnwrapped list is rolled out flat too")
        .contains("\"top\":\"t\"")
        .contains("\"n\":\"x\"")
        .contains("\"e2\":\"z\"");
  }

  @Test
  void emptyExtensionListContributesNothing() {
    String json = toJson(new ListOuter(PlainText.of("t"), List.of()));

    assertThat(json)
        .as("an empty @OutputUnwrapped list leaves no dangling key")
        .isEqualTo("{\"x\":{\"top\":\"t\"}}");
  }

  static final class SimplePojo {
    public Text getName() {
      return PlainText.of("n");
    }
  }

  @Test
  void mapsNonRecordPojoViaGetters() {
    String json = toJson(new SimplePojo());

    assertThat(json)
        .as("a non-record POJO is mapped via its bean getters")
        .isEqualTo("{\"x\":{\"name\":\"n\"}}");
  }
}

package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.value.media.FocalPoint;
import com.sitepark.ies.aggregator.value.media.Hash;
import com.sitepark.ies.aggregator.value.media.HashAlgorithm;
import com.sitepark.ies.aggregator.value.media.Image;
import com.sitepark.ies.aggregator.value.media.ImageMetadata;
import com.sitepark.ies.aggregator.value.media.Media;
import com.sitepark.ies.aggregator.value.media.ZipArchive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/**
 * The scenario archive follows {@code sp:zip}: the name rule, the place below the page, the real
 * size of a level-0 archive, and nothing for what the store would not hold.
 */
class ScenarioZipArchivesTest {

  /** A standalone medium (object 2000, asset 11) and an upload in a field of a page (3000, 12). */
  private static final Map<String, Object> ENTRIES =
      Map.of(
          "1000", Map.of("id", 1000),
          "2000", Map.of("id", 2000, "media", Map.of("id", 11)),
          "3000",
              Map.of(
                  "id", 3000, "content", Map.of("sp_upload", Map.of("media", Map.of("id", 12)))));

  private static ScenarioZipArchives archives(Map<String, Object> entries) {
    Repository repository = new Repository(entries);
    ChannelProvider channels =
        new ScenarioChannelProvider(repository, null, ScenarioChannelConfig.EMPTY);
    return new ScenarioZipArchives(repository, channels);
  }

  private static Image image(int objectId, int id, String filename, long size) {
    return new Image(
        objectId,
        id,
        filename,
        filename,
        "image/jpeg",
        size,
        new Hash(HashAlgorithm.SHA_256, "x"),
        new ImageMetadata(null, null, null, null, null, FocalPoint.CENTER, null),
        null,
        10,
        10,
        null);
  }

  /** The bytes of a real archive of entries of the given sizes, written the way sp:zip writes. */
  private static long sizeOfARealArchive(Map<String, Integer> entries) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.setLevel(0);
      for (Map.Entry<String, Integer> entry : entries.entrySet()) {
        writeEntry(zip, entry.getKey(), entry.getValue());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes.size();
  }

  private static void writeEntry(ZipOutputStream zip, String name, int size) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    // sp:zip copies with a buffer of 1024 bytes, and the framing follows the writes
    byte[] buffer = new byte[1024];
    int left = size;
    while (left > 0) {
      int length = Math.min(left, buffer.length);
      zip.write(buffer, 0, length);
      left -= length;
    }
    zip.closeEntry();
  }

  @Test
  void writesTheArchiveBelowThePageOfTheResource() {
    ScenarioZipArchives zips = archives(ENTRIES);

    ZipArchive archive =
        zips.create("Rathaus Süd", List.<Media>of(image(2000, 11, "a.jpg", 10))).orElseThrow();

    assertThat(archive.filename())
        .as("the name goes through the sp:zip rule: the umlaut and the blank become underscores")
        .isEqualTo("Rathaus_S_d.zip");
    assertThat(archive.url().toString())
        .as("the archive lies in the zips folder of the media directory of the page")
        .isEqualTo("/object/1000.media/zips/Rathaus_S_d.zip");
  }

  @Test
  void answersTheSizeOfARealArchiveOfMediaOfThoseSizes() {
    ScenarioZipArchives zips = archives(ENTRIES);
    Map<String, Integer> sizes = new LinkedHashMap<>();
    sizes.put("a.jpg", 10);
    sizes.put("b.jpg", 70000);

    ZipArchive archive =
        zips.create(
                "g", List.<Media>of(image(2000, 11, "a.jpg", 10), image(3000, 12, "b.jpg", 70000)))
            .orElseThrow();

    assertThat(archive.size())
        .as("level 0 still frames the data, so the size is that of the bytes a real archive has")
        .isEqualTo(sizeOfARealArchive(sizes));
    assertThat(zips.created().getFirst().entries())
        .as("the entries are the file names of the media, in order")
        .containsExactly("a.jpg", "b.jpg");
  }

  @Test
  void leavesOutAMediumTheStoreDoesNotHold() {
    ScenarioZipArchives zips = archives(ENTRIES);

    zips.create(
        "g",
        List.<Media>of(
            image(2000, 11, "a.jpg", 10),
            image(2000, 99, "ghost.jpg", 10),
            image(4000, 11, "elsewhere.jpg", 10)));

    assertThat(zips.created().getFirst().entries())
        .as("an asset id the object does not hold, and an object the scenario lacks, are unknown")
        .containsExactly("a.jpg");
  }

  @Test
  void answersNothingForTwoMediaOfOneFileName() {
    ScenarioZipArchives zips = archives(ENTRIES);

    assertThat(
            zips.create(
                "g", List.<Media>of(image(2000, 11, "a.jpg", 10), image(3000, 12, "a.jpg", 10))))
        .as("ZipOutputStream refuses the duplicate entry, and sp:zip fails with it")
        .isEmpty();
    assertThat(zips.created()).as("a failed archive is not recorded").isEmpty();
  }

  @Test
  void answersNothingWhereTheChannelPublishesNoPage() {
    ScenarioZipArchives zips =
        archives(
            Map.of("1000", Map.of("id", 1000, "published", false), "2000", Map.of("id", 2000)));

    assertThat(zips.create("g", List.<Media>of()))
        .as("the page is the source; unpublished, the publisher has no file to put the archive by")
        .isEmpty();
  }

  @Test
  void anEmptyListStillYieldsAnEmptyArchive() {
    ScenarioZipArchives zips = archives(ENTRIES);

    ZipArchive archive = zips.create("g", List.<Media>of()).orElseThrow();

    assertThat(archive.size())
        .as("sp:zip writes an archive without entries, only its end record")
        .isEqualTo(sizeOfARealArchive(Map.of()));
  }
}

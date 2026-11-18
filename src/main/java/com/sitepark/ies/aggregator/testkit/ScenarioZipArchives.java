package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.port.ZipArchives;
import com.sitepark.ies.aggregator.value.media.EmptyMedia;
import com.sitepark.ies.aggregator.value.media.Media;
import com.sitepark.ies.aggregator.value.media.ZipArchive;
import com.sitepark.ies.aggregator.value.uri.PlainUri;
import com.sitepark.ies.aggregator.value.uri.UriTarget;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * {@link ZipArchives} of a scenario: builds the archive the way {@code sp:zip} does - the real
 * {@link ZipOutputStream} at level 0, one entry per medium under its file name - but into a sink
 * that only counts, since a scenario's media have a size and no content. The size it answers is
 * therefore the size of an archive of media of exactly those sizes, not a made-up number.
 *
 * <p>No more generous than production, where the same things go wrong:
 *
 * <ul>
 *   <li>the file name is {@link ZipArchive#filenameOf(String)} of the name, as in {@code sp:zip};
 *   <li>a medium the scenario does not hold - no standalone medium of that id, no upload of that
 *       id in the entry of its object - is not known to the store and is left out;
 *   <li>two media of one file name fail the archive, as {@code ZipOutputStream} fails the duplicate
 *       entry: the answer is empty;
 *   <li>the archive lies below the page of the resource, {@code <page>.media/zips/<file name>},
 *       and without a page there is no archive: the page of a scenario is its source, and a source
 *       the channel does not publish has no page URL, so the answer is empty, as it is where the
 *       publisher has no file for the object.
 * </ul>
 *
 * <p>Every archive created is recorded, so a test can ask what the publisher would have written.
 */
final class ScenarioZipArchives implements ZipArchives {

  /** The folder of the media directory the archives lie in, as in {@code sp:zip}. */
  private static final String ZIP_FOLDER = "zips";

  /**
   * The size of the pieces {@code sp:zip} copies a file in. It matters: the deflater frames every
   * write, so the size of the archive depends on the way the data is cut.
   */
  private static final int CHUNK = 1024;

  private final Repository repository;
  private final ChannelProvider channelProvider;
  private final List<CreatedZipArchive> created = new ArrayList<>();

  ScenarioZipArchives(Repository repository, ChannelProvider channelProvider) {
    this.repository = repository;
    this.channelProvider = channelProvider;
  }

  @Override
  public Optional<ZipArchive> create(String name, List<? extends Media> media) {
    Optional<PlainUri> page =
        this.channelProvider
            .current()
            .resolveUri(UriTarget.ofObject(Integer.parseInt(ScenarioContext.SOURCE_ID)));
    if (page.isEmpty()) {
      return Optional.empty();
    }
    List<Media> known = media.stream().filter(this::exists).map(Media.class::cast).toList();
    CountingStream counter = new CountingStream();
    try (ZipOutputStream zip = new ZipOutputStream(counter)) {
      zip.setLevel(0);
      for (Media medium : known) {
        addEntry(zip, medium);
      }
    } catch (IOException e) {
      return Optional.empty();
    }
    String filename = ZipArchive.filenameOf(name);
    ZipArchive archive =
        new ZipArchive(
            filename,
            PlainUri.of(page.get() + ".media/" + ZIP_FOLDER + "/" + filename),
            counter.count);
    this.created.add(new CreatedZipArchive(archive, known.stream().map(Media::filename).toList()));
    return Optional.of(archive);
  }

  List<CreatedZipArchive> created() {
    return List.copyOf(this.created);
  }

  private boolean exists(Media medium) {
    if (medium instanceof EmptyMedia) {
      return false;
    }
    String objectId = Integer.toString(medium.objectId());
    Integer standalone = this.repository.mediaIdOfEntry(objectId);
    return (standalone != null && standalone == medium.id())
        || this.repository.embeddedMediaPath(objectId, medium.id()) != null;
  }

  /** One entry under the file name of the medium, as many zero bytes as the medium has. */
  private static void addEntry(ZipOutputStream zip, Media medium) throws IOException {
    zip.putNextEntry(new ZipEntry(medium.filename()));
    byte[] chunk = new byte[CHUNK];
    long left = medium.fileSize();
    while (left > 0) {
      int length = (int) Math.min(left, CHUNK);
      zip.write(chunk, 0, length);
      left -= length;
    }
    zip.closeEntry();
  }

  /** A sink that keeps only how many bytes went in. */
  private static final class CountingStream extends OutputStream {

    private long count;

    @Override
    public void write(int b) {
      this.count++;
    }

    @Override
    public void write(byte[] b, int off, int len) {
      this.count += len;
    }
  }
}

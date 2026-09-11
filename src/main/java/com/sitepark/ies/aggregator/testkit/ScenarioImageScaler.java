package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ImageScaler;
import com.sitepark.ies.aggregator.value.media.Image;
import com.sitepark.ies.aggregator.value.media.scaling.ComputedSize;
import com.sitepark.ies.aggregator.value.media.scaling.Encoded;
import com.sitepark.ies.aggregator.value.media.scaling.FitMode;
import com.sitepark.ies.aggregator.value.media.scaling.Format;
import com.sitepark.ies.aggregator.value.media.scaling.ScaleImageRequest;
import com.sitepark.ies.aggregator.value.media.scaling.ScaleImageTarget;
import com.sitepark.ies.aggregator.value.media.scaling.ScaledImage;
import com.sitepark.ies.aggregator.value.uri.PlainUri;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic {@link ImageScaler} test double. It does not scale real pixels; for every requested
 * target it produces a {@link ScaledImage} whose dimensions are the computed size rounded to whole
 * pixels and whose URLs are derived from those dimensions and the requested formats, so a scenario's
 * scaled sources are stable and assertable against a golden file.
 *
 * <p>A request without an explicit format is answered with a single JPEG encoding, mirroring an
 * engine that derives the output format from the source image.
 *
 * <p>The one part of real scaling it does reproduce is {@link FitMode#CONTAIN}, because that is the
 * only mode whose result differs from the requested box: it fits the source inside the box instead
 * of filling it. The other three reach the box exactly by definition, so returning it verbatim is
 * faithful for them. Without this, a zoom of a 3000×2250 source into a 1920×1920 box would be
 * recorded as 1920×1920 in a golden file while production produces 1920×1440.
 */
final class ScenarioImageScaler implements ImageScaler {

  @Override
  public List<ScaledImage> scale(ScaleImageRequest request) {
    List<ScaledImage> scaled = new ArrayList<>();
    for (ScaleImageTarget target : request.targets()) {
      ComputedSize size = fit(target.size(), request.image());
      int width = (int) Math.round(size.width());
      int height = (int) Math.round(size.height());
      scaled.add(ScaledImage.of(width, height, encodings(request.formats(), width, height)));
    }
    return scaled;
  }

  /** Fits the requested box to the source's aspect ratio, for the one mode where that matters. */
  private static ComputedSize fit(ComputedSize size, Image image) {
    if (size.fitMode() != FitMode.CONTAIN || image.width() <= 0 || image.height() <= 0) {
      return size;
    }
    double factor = Math.min(size.width() / image.width(), size.height() / image.height());
    return ComputedSize.of(image.width() * factor, image.height() * factor, FitMode.CONTAIN);
  }

  private static List<Encoded> encodings(List<Format> formats, int width, int height) {
    if (formats.isEmpty()) {
      return List.of(encoded(Format.JPEG, "jpg", width, height));
    }
    List<Encoded> encodings = new ArrayList<>();
    for (Format format : formats) {
      encodings.add(encoded(format, format.getName(), width, height));
    }
    return encodings;
  }

  private static Encoded encoded(Format format, String suffix, int width, int height) {
    return Encoded.of(format, PlainUri.of("/scaled/" + width + "x" + height + "." + suffix));
  }
}

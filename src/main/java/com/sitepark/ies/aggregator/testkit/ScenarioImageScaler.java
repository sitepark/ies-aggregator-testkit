package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ImageScaler;
import com.sitepark.ies.aggregator.value.media.Image;
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
 * target it produces a {@link ScaledImage} whose dimensions are those of production and whose URLs
 * are derived from those dimensions and the requested formats, so a scenario's scaled sources are
 * stable and assertable against a golden file.
 *
 * <p>It follows the production path step for step: a fractional target size is truncated to whole
 * pixels (ies-3 casts to {@code int}), a dimension of 0 is derived from the source ratio and the
 * result is computed like imgfile's {@code DimensionCalculator} does, with the same float and
 * {@code Math.round} arithmetic. {@link FitMode#CONTAIN} fits the source inside the box, never
 * enlarging it; the other three modes reach the box exactly. A zoom of a 3000×2250 source into a
 * 1920×1920 box is therefore 1920×1440, as in production.
 *
 * <p>A request without an explicit format is answered with a single JPEG encoding, mirroring an
 * engine that derives the output format from the source image.
 */
final class ScenarioImageScaler implements ImageScaler {

  @Override
  public List<ScaledImage> scale(ScaleImageRequest request) {
    List<ScaledImage> scaled = new ArrayList<>();
    for (ScaleImageTarget target : request.targets()) {
      // Production hands the engine whole pixels: ies-3 InternalImageScaler casts (int), so a
      // fractional size is truncated, not rounded.
      int desiredWidth = (int) target.size().width();
      int desiredHeight = (int) target.size().height();
      int[] size = size(request.image(), desiredWidth, desiredHeight, target.size().fitMode());
      scaled.add(ScaledImage.of(size[0], size[1], encodings(request.formats(), size[0], size[1])));
    }
    return scaled;
  }

  /**
   * Computes the output size the way imgfile's {@code ImageIO.getScaleImage} does without scale
   * steps: {@code DimensionCalculator.calculate()} followed by {@code LegacyPropertiesCalculator}.
   * Only {@link FitMode#CONTAIN} (neither fit nor padding) takes the size from the dimension
   * calculator; the other three modes reach the desired box.
   */
  private static int[] size(Image image, int desiredWidth, int desiredHeight, FitMode fitMode) {
    int sourceWidth = image.width();
    int sourceHeight = image.height();
    int width = desiredWidth;
    int height = desiredHeight;

    // AbstractDimesionCalculator.calculateMissingDimension: float arithmetic, Math.round, capped
    // at the source size.
    if (width == 0 && height == 0) {
      throw new IllegalArgumentException("desiredWidth and desiredHeight are 0");
    }
    if (width == 0) {
      width =
          Math.min(Math.round(height * ((float) sourceWidth / (float) sourceHeight)), sourceWidth);
    }
    if (height == 0) {
      height =
          Math.min(Math.round(width / ((float) sourceWidth / (float) sourceHeight)), sourceHeight);
    }
    if (fitMode != FitMode.CONTAIN || sourceWidth <= 0 || sourceHeight <= 0) {
      return new int[] {width, height};
    }

    // DimensionCalculator.calculateDimension with the lower factor (fit is false); a scale above 1
    // is only kept for SVG, which has no pixel size to exceed.
    double factorByWidth = (double) width / (double) sourceWidth;
    double factorByHeight = (double) height / (double) sourceHeight;
    double scale = factorByWidth < factorByHeight ? factorByWidth : factorByHeight;
    if (scale > 1 && !"image/svg+xml".equals(image.mimeType())) {
      scale = 1;
    }
    // minimumCorrection
    return new int[] {
      Math.max(1, Math.round((float) sourceWidth * (float) scale)),
      Math.max(1, Math.round((float) sourceHeight * (float) scale))
    };
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

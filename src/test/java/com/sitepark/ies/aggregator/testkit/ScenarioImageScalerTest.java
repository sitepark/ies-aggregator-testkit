package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.aggregator.value.media.FocalPoint;
import com.sitepark.ies.aggregator.value.media.Hash;
import com.sitepark.ies.aggregator.value.media.HashAlgorithm;
import com.sitepark.ies.aggregator.value.media.Image;
import com.sitepark.ies.aggregator.value.media.ImageMetadata;
import com.sitepark.ies.aggregator.value.media.scaling.ComputedSize;
import com.sitepark.ies.aggregator.value.media.scaling.FitMode;
import com.sitepark.ies.aggregator.value.media.scaling.ScaleImageRequest;
import com.sitepark.ies.aggregator.value.media.scaling.ScaleImageTarget;
import com.sitepark.ies.aggregator.value.media.scaling.ScaledImage;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the scenario scaler completes a dimension of 0 the way the engine does, so a
 * scenario shows what production produces: the engine derives it from the source ratio, rounds it
 * and never exceeds the source size.
 */
class ScenarioImageScalerTest {

  private static Image image(int width, int height) {
    return new Image(
        1,
        2,
        "a.jpg",
        "a.jpg",
        "image/jpeg",
        1L,
        new Hash(HashAlgorithm.SHA_256, "x"),
        new ImageMetadata(null, null, null, null, null, FocalPoint.CENTER, null),
        null,
        width,
        height,
        null);
  }

  private static ScaledImage scale(Image image, double width, double height, FitMode fitMode) {
    List<ScaledImage> scaled =
        new ScenarioImageScaler()
            .scale(
                ScaleImageRequest.of(
                    image,
                    List.of(ScaleImageTarget.of(ComputedSize.of(width, height, fitMode), null))));
    return scaled.getFirst();
  }

  @Test
  void derivesTheWidthFromTheHeightAndRoundsIt() {
    ScaledImage scaled = scale(image(3000, 1295), 0, 440, FitMode.CONTAIN);

    assertThat(scaled.height()).as("the given height is kept").isEqualTo(440);
    assertThat(scaled.width()).as("440 * 3000 / 1295 = 1019.3, rounded").isEqualTo(1019);
  }

  @Test
  void derivesTheHeightFromTheWidth() {
    ScaledImage scaled = scale(image(1000, 2316), 440, 0, FitMode.COVER);

    assertThat(scaled.width()).isEqualTo(440);
    assertThat(scaled.height()).as("440 * 2316 / 1000 = 1019.04, rounded").isEqualTo(1019);
  }

  @Test
  void roundsAHalfUpWhereTruncatingWouldLoseAPixel() {
    ScaledImage scaled = scale(image(1000, 431), 440, 0, FitMode.COVER);

    assertThat(scaled.height())
        .as("440 * 431 / 1000 = 189.64: the engine rounds to 190, it does not truncate to 189")
        .isEqualTo(190);
  }

  @Test
  void derivesInFloatPrecisionSoAHalfRoundsUp() {
    ScaledImage scaled = scale(image(1600, 900), 600, 0, FitMode.CONTAIN);

    assertThat(scaled.width()).as("the given width survives the fit").isEqualTo(600);
    assertThat(scaled.height())
        .as("600 / (1600f / 900f) is 337.5 in float arithmetic, which rounds to 338")
        .isEqualTo(338);
  }

  @Test
  void neverExceedsTheSourceSize() {
    ScaledImage scaled = scale(image(100, 50), 400, 0, FitMode.COVER);

    assertThat(scaled.height()).as("derived 200 is capped to the source height").isEqualTo(50);
  }

  @Test
  void leavesASetDimensionAlone() {
    ScaledImage scaled = scale(image(3000, 2000), 232, 155, FitMode.COVER);

    assertThat(scaled.width()).isEqualTo(232);
    assertThat(scaled.height()).isEqualTo(155);
  }

  @Test
  void keepsTheGivenDimensionOfADerivedBoxForContain() {
    ScaledImage scaled = scale(image(3760, 2454), 393, 0, FitMode.CONTAIN);

    assertThat(scaled.width()).as("the fit must not take a pixel off the width").isEqualTo(393);
    assertThat(scaled.height())
        .as("393 * 2454 / 3760 = 256.5 in float, rounded")
        .isBetween(256, 257);
  }

  @Test
  void rejectsASizeWithoutAnyDimension() {
    Image image = image(3000, 2000);

    assertThatThrownBy(() -> scale(image, 0, 0, FitMode.COVER))
        .as("the engine refuses a size that has neither dimension")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fitsTheDerivedBoxForContain() {
    ScaledImage scaled = scale(image(3000, 2250), 1920, 0, FitMode.CONTAIN);

    assertThat(scaled.width()).isEqualTo(1920);
    assertThat(scaled.height()).isEqualTo(1440);
  }
}

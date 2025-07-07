/*
 * Copyright 2025 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.dgtldcmnt.compression;

import com.epam.digital.data.platform.dgtldcmnt.dto.ImageCompressorParameters;
import com.epam.digital.data.platform.dgtldcmnt.exception.FileCompressionException;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardImageCompressorTest {

  private static final String FILE_NAME = "test.jpg";

  private static final int DEFAULT_MAX_WIDTH = 1024;
  private static final int DEFAULT_MAX_HEIGHT = 768;
  private static final int DEFAULT_COMPRESSION_QUALITY = 80;
  private static final DataSize DEFAULT_MIN_COMPRESSIBLE_FILE_SIZE = DataSize.ofBytes(5);

  private StandardImageCompressor imageCompressor;

  @Mock
  private Detector defaultDetector;

  @BeforeEach
  void setUp() throws IOException {
    imageCompressor = new StandardImageCompressor(
        defaultDetector,
        DEFAULT_MAX_WIDTH,
        DEFAULT_MAX_HEIGHT,
        DEFAULT_COMPRESSION_QUALITY,
        DEFAULT_MIN_COMPRESSIBLE_FILE_SIZE
    );
    
    // Setup mock behavior for the detector to return image/jpeg media type for tests
    lenient().when(defaultDetector.detect(any(InputStream.class), any(Metadata.class)))
        .thenReturn(MediaType.parse("image/jpeg"));
  }

  @Test
  @DisplayName("Should compress image using default parameters")
  void shouldCompressImageWithDefaultParameters() throws Exception {
    // given
    BufferedImage originalImage = createTestImage(2048, 1536);
    var fileData = convertToInputStream(originalImage);

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, ImageCompressorParameters.builder().build());

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);
    assertEquals(DEFAULT_MAX_WIDTH, compressedImage.getWidth());
    assertEquals(DEFAULT_MAX_HEIGHT, compressedImage.getHeight());

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @Test
  @DisplayName("Should compress image using custom parameters")
  void shouldCompressImageWithCustomParameters() throws Exception {
    // given
    BufferedImage originalImage = createTestImage(2000, 1500);
    var fileData = convertToInputStream(originalImage);

    int customWidth = 800;
    int customHeight = 600;
    int customQuality = 50;

    ImageCompressorParameters parameters = ImageCompressorParameters.builder()
        .imageMaxWidth(customWidth)
        .imageMaxHeight(customHeight)
        .compressionQuality(customQuality)
        .build();

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, parameters);

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);
    assertEquals(customWidth, compressedImage.getWidth());
    assertEquals(customHeight, compressedImage.getHeight());

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @Test
  @DisplayName("Should maintain original size when image is smaller than max dimensions")
  void shouldMaintainOriginalSizeForSmallImages() throws Exception {
    // given
    int originalWidth = 500;
    int originalHeight = 300;
    BufferedImage originalImage = createTestImage(originalWidth, originalHeight);
    var fileData = convertToInputStream(originalImage);

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, ImageCompressorParameters.builder().build());

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);
    assertEquals(originalWidth, compressedImage.getWidth());
    assertEquals(originalHeight, compressedImage.getHeight());

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @Test
  @DisplayName("Should maintain aspect ratio for landscape images")
  void shouldMaintainAspectRatioForLandscapeImages() throws Exception {
    // given
    int originalWidth = 2000;
    int originalHeight = 1000;
    double aspectRatio = (double) originalWidth / originalHeight;

    BufferedImage originalImage = createTestImage(originalWidth, originalHeight);
    var fileData = convertToInputStream(originalImage);

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, ImageCompressorParameters.builder().build());

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);

    double compressedAspectRatio = (double) compressedImage.getWidth() / compressedImage.getHeight();
    assertEquals(aspectRatio, compressedAspectRatio, 0.01); // Allow small rounding differences
    assertTrue(compressedImage.getWidth() <= DEFAULT_MAX_WIDTH);
    assertTrue(compressedImage.getHeight() <= DEFAULT_MAX_HEIGHT);

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @Test
  @DisplayName("Should maintain aspect ratio for portrait images")
  void shouldMaintainAspectRatioForPortraitImages() throws Exception {
    // given
    int originalWidth = 1000;
    int originalHeight = 2000;
    double aspectRatio = (double) originalWidth / originalHeight;

    BufferedImage originalImage = createTestImage(originalWidth, originalHeight);
    var fileData = convertToInputStream(originalImage);

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, ImageCompressorParameters.builder().build());

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);

    double compressedAspectRatio = (double) compressedImage.getWidth() / compressedImage.getHeight();
    assertEquals(aspectRatio, compressedAspectRatio, 0.01); // Allow small rounding differences
    assertTrue(compressedImage.getWidth() <= DEFAULT_MAX_WIDTH);
    assertTrue(compressedImage.getHeight() <= DEFAULT_MAX_HEIGHT);

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @Test
  @DisplayName("Should throw exception when input is not a valid image")
  void shouldThrowExceptionWhenInputIsNotValidImage() {
    // given
    byte[] invalidImageData = "This is not an image".getBytes();
    BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(invalidImageData));

    // when & then
    assertThrows(FileCompressionException.class, () ->
        imageCompressor.compress(FILE_NAME, inputStream, ImageCompressorParameters.builder().build()));
  }

  @Test
  @DisplayName("Should handle null compression parameters")
  void shouldHandleNullCompressionParameters() throws Exception {
    // given
    BufferedImage originalImage = createTestImage(1500, 1000);
    var fileData = convertToInputStream(originalImage);

    // when
    var compressedData = imageCompressor.compress(FILE_NAME, fileData, ImageCompressorParameters.builder().build());

    // then
    assertNotNull(compressedData);
    BufferedImage compressedImage = ImageIO.read(compressedData);
    assertNotNull(compressedImage);
    assertTrue(compressedImage.getWidth() <= DEFAULT_MAX_WIDTH);
    assertTrue(compressedImage.getHeight() <= DEFAULT_MAX_HEIGHT);

    verifyImageContentPreserved(originalImage, compressedImage);
  }

  @ParameterizedTest
  @DisplayName("Should validate supported file extensions")
  @ValueSource(strings = {"image.jpg", "image.jpeg", "image.png"})
  void shouldValidateSupportedExtensions(String filename) throws IOException {
    // given
    int fileSize = 10;
    BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[fileSize]));
    
    // when (setup mock for specific media type)
    MediaType mediaType;
    if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
      mediaType = MediaType.parse("image/jpeg");
    } else if (filename.endsWith(".png")) {
      mediaType = MediaType.parse("image/png");
    } else {
      mediaType = MediaType.parse("image/unsupported");
    }
    
    when(defaultDetector.detect(eq(inputStream), any(Metadata.class))).thenReturn(mediaType);
    
    // then
    assertTrue(imageCompressor.canCompress(filename,fileSize, inputStream));
  }

  @ParameterizedTest
  @DisplayName("Should reject unsupported file extensions")
  @ValueSource(strings = {"image.pdf", "image.doc", "image.txt", "image.gif", "image.bmp"})
  void shouldRejectUnsupportedExtensions(String filename) throws IOException {
    // given
    int fileSize = 10;
    BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[10]));
    
    // when (setup mock to return unsupported media type)
    MediaType mediaType;
    if (filename.endsWith(".pdf")) {
      mediaType = MediaType.parse("application/pdf");
    } else if (filename.endsWith(".doc")) {
      mediaType = MediaType.parse("application/msword");
    } else if (filename.endsWith(".txt")) {
      mediaType = MediaType.parse("text/plain");
    } else {
      mediaType = MediaType.parse("image/unsupported");
    }
    
    when(defaultDetector.detect(eq(inputStream), any(Metadata.class))).thenReturn(mediaType);
    
    // then
    assertFalse(imageCompressor.canCompress(filename, fileSize, inputStream));
  }

  /**
   * Creates a test image with the specified dimensions.
   *
   * @param width  the width of the image
   * @param height the height of the image
   * @return a BufferedImage instance
   */
  private BufferedImage createTestImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // Draw a gradient background
    g2d.setColor(Color.WHITE);
    g2d.fillRect(0, 0, width, height);
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 0, width / 2, height / 2);
    g2d.setColor(Color.RED);
    g2d.fillRect(width / 2, 0, width / 2, height / 2);
    g2d.setColor(Color.GREEN);
    g2d.fillRect(0, height / 2, width / 2, height / 2);
    g2d.setColor(Color.YELLOW);
    g2d.fillRect(width / 2, height / 2, width / 2, height / 2);

    g2d.dispose();
    return image;
  }

  /**
   * Verifies that the content of two images is preserved by checking color at sampled points.
   * This accounts for minor differences due to compression.
   *
   * @param original   The original image
   * @param compressed The compressed image
   */
  private void verifyImageContentPreserved(@NonNull BufferedImage original, @NonNull BufferedImage compressed) {
    // Sample points proportionally from both images
    int numSamplePoints = 10;
    for (int i = 1; i <= numSamplePoints; i++) {
      // Calculate sample points proportionally
      int originalX = (original.getWidth() * i) / (numSamplePoints + 1);
      int originalY = (original.getHeight() * i) / (numSamplePoints + 1);
      int compressedX = (compressed.getWidth() * i) / (numSamplePoints + 1);
      int compressedY = (compressed.getHeight() * i) / (numSamplePoints + 1);

      // Get RGB colors
      int originalRGB = original.getRGB(originalX, originalY);
      int compressedRGB = compressed.getRGB(compressedX, compressedY);

      // Allow some tolerance for color differences due to compression
      Color originalColor = new Color(originalRGB);
      Color compressedColor = new Color(compressedRGB);

      // Check if colors are similar (allowing some compression artifacts)
      assertTrue(areColorsSimilar(originalColor, compressedColor),
          "Colors should be similar at sample point " + i);
    }
  }

  /**
   * Determines if two colors are similar within the tolerance 5.
   *
   * @param c1 First color
   * @param c2 Second color
   * @return true if the colors are similar within tolerance
   */
  private boolean areColorsSimilar(@NonNull Color c1, @NonNull Color c2) {
    return Math.abs(c1.getRed() - c2.getRed()) <= 5 &&
        Math.abs(c1.getGreen() - c2.getGreen()) <= 5 &&
        Math.abs(c1.getBlue() - c2.getBlue()) <= 5;
  }

  private BufferedInputStream convertToInputStream(BufferedImage image) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ImageIO.write(image, "jpeg", os);
    return new BufferedInputStream(new ByteArrayInputStream(os.toByteArray()));
  }
}
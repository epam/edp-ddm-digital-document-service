/*
 * Copyright 2025 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.epam.digital.data.platform.dgtldcmnt.util.ImageProcessingUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of AbstractImageCompressor that compresses image files.
 * Supports resizing and quality adjustment of images.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandardImageCompressor implements ImageCompressor {

  private static final List<MediaType> SUPPORTED_TYPES = List.of(
    MediaType.parse("image/jpeg"),
    MediaType.parse("image/png")
  );

  @Qualifier("default-detector")
  private final Detector defaultDetector;

  @Getter
  @Value("${digital-documents.compression-params.image-max-width:0}")
  private final int imageMaxWidth;
  @Getter
  @Value("${digital-documents.compression-params.image-max-height:0}")
  private final int imageMaxHeight;
  @Getter
  @Value("${digital-documents.compression-params.compression-quality:-1}")
  private final int compressionQuality;
  @Getter
  @Value("${digital-documents.compression-params.min-compressible-file-size:1MB}")
  private final DataSize minCompressibleFileSize;

  /**
   * Compresses an image based on the provided parameters.
   * If no compression is needed (dimensions are within limits and quality is 100),
   * the original stream is returned.
   *
   * @param filename    the name of the file to be compressed
   * @param inputStream the input stream containing the image data
   * @param parameters  optional compression parameters that can override defaults
   * @return a new input stream with the compressed image data or the original stream if no compression needed
   * @throws FileCompressionException if compression fails
   */
  @Override
  public @NonNull BufferedInputStream compress(@NonNull String filename, @NonNull BufferedInputStream inputStream, @NonNull ImageCompressorParameters parameters) throws FileCompressionException {

    int imageMaxWidth = Objects.requireNonNullElse(parameters.getImageMaxWidth(), this.imageMaxWidth);
    int imageMaxHeight = Objects.requireNonNullElse(parameters.getImageMaxHeight(), this.imageMaxHeight);
    int compressionQuality = Objects.requireNonNullElse(parameters.getCompressionQuality(), this.compressionQuality);
    // Skip compression if no constraints are applied
    if (imageMaxWidth == 0 && imageMaxHeight == 0 && compressionQuality < 0) {
      log.debug("Skipping image compression as no constraints are applied");
      return inputStream;
    }

    try {
      inputStream.mark(Integer.MAX_VALUE);
      BufferedImage inputImage = ImageIO.read(inputStream);
      inputStream.reset();
      if (inputImage == null) {
        throw new FileCompressionException("Unable to read image from input stream");
      }

      boolean hasAlpha = inputImage.getColorModel().hasAlpha();
      ImageProcessingUtils.ImageDimensions dims =
        ImageProcessingUtils.calculateNewImageDimensions(inputImage, imageMaxWidth, imageMaxHeight);

      // Skip if no changes needed
      if (dims.getWidth() == inputImage.getWidth()
        && dims.getHeight() == inputImage.getHeight()
        && compressionQuality == 100) {
        return inputStream;
      }

      BufferedImage resized = ImageProcessingUtils.resizeImage(
        inputImage, dims, hasAlpha);

      float qualityFactor = compressionQuality / 100f;
      MediaType mediaType = detectMediaType(filename, inputStream);

      return compressImage(mediaType, resized, qualityFactor);
    } catch (IOException exception) {
      throw new FileCompressionException("Failed to compress image", exception);
    }
  }

  /**
   * Compresses an image with specified compression quality.
   *
   * @param mediaType          the MediaType of the image to compress, cannot be null
   * @param image              the BufferedImage to compress, cannot be null
   * @param compressionQuality the quality of compression (between 0.0f and 1.0f), cannot be null
   * @return the compressed image as a byte array
   * @throws IOException           if an I/O error occurs during compression
   */
  private BufferedInputStream compressImage(
    @lombok.NonNull MediaType mediaType, @NonNull BufferedImage image, float compressionQuality) throws IOException {
    String format = mediaType.getSubtype();
    byte[] out = null;
    if ("jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format)) {
      out = ImageProcessingUtils.writeImageAsJpeg(image, compressionQuality);
    } else if ("png".equalsIgnoreCase(format)) {
      out = ImageProcessingUtils.writeImageAsPng(image);
    } else {
      // Fallback for other formats
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, format, outputStream);
      return new BufferedInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    }
    return new BufferedInputStream(new ByteArrayInputStream(out));
  }

  @Override
  public boolean canCompress(@NonNull String filename, @NonNull long fileSize, @NonNull BufferedInputStream inputStream) {
    if (minCompressibleFileSize.toBytes() > fileSize) {
      return false;
    }
    try {
      var mediaType = detectMediaType(filename, inputStream);
      return SUPPORTED_TYPES.contains(mediaType);
    } catch (IOException exception) {
      throw new FileCompressionException("Failed to detect file type", exception);
    }
  }

  private MediaType detectMediaType(String filename, BufferedInputStream inputStream) throws IOException {
    var metadata = new Metadata();
    metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
    return defaultDetector.detect(inputStream, metadata);
  }
}
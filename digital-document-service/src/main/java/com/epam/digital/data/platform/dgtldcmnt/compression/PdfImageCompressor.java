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
import com.epam.digital.data.platform.dgtldcmnt.dto.ImageEntry;
import com.epam.digital.data.platform.dgtldcmnt.exception.FileCompressionException;
import com.epam.digital.data.platform.dgtldcmnt.util.ImageProcessingUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of ImageCompressor that compresses PDF files by optimizing embedded images.
 * <p>
 * This implementation processes the PDF document and its embedded images entirely in memory
 * for improved efficiency and security.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfImageCompressor implements ImageCompressor {

  private static final List<MediaType> SUPPORTED_TYPES = List.of(MediaType.parse("application/pdf"));

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
   * Compresses PDF by reducing the quality and dimensions of embedded images.
   * All processing is done in-memory.
   *
   * @param fileName    the name of the file to be compressed, cannot be null
   * @param inputStream the input stream containing the PDF to be compressed, cannot be null
   * @param parameters  compression parameters that control the compression process, can be null for default settings
   * @return an input stream containing the compressed PDF data
   * @throws FileCompressionException if any error occurs during compression
   */
  @Override
  public @NonNull BufferedInputStream compress(@NonNull String fileName, @NonNull BufferedInputStream inputStream, @NonNull ImageCompressorParameters parameters) throws FileCompressionException {

    int imageMaxWidth = Objects.requireNonNullElse(parameters.getImageMaxWidth(), this.imageMaxWidth);
    int imageMaxHeight = Objects.requireNonNullElse(parameters.getImageMaxHeight(), this.imageMaxHeight);
    int compressionQuality = Objects.requireNonNullElse(parameters.getCompressionQuality(), this.compressionQuality);

    if (imageMaxWidth == 0 && imageMaxHeight == 0 && compressionQuality < 0) {
      return inputStream;
    }

    PDDocument document;
    try {
      inputStream.mark(inputStream.available() + 1);
      document = PDDocument.load(inputStream);
      inputStream.reset();
    } catch (IOException ex) {
      throw new FileCompressionException("Failed to compress PDF file", ex);
    }

    try {
      Map<ImageEntry, List<PDResources>> images = collectImagesFromPdf(document);
      for (var imageEntry : images.entrySet()) {
        var oldImage = imageEntry.getKey();
        var resources = imageEntry.getValue();
        var newImage = compressImageEntry(document, oldImage, imageMaxWidth, imageMaxHeight, compressionQuality);
        for (var resource : resources) {
          resource.put(newImage.getCOSName(), newImage.getImage());
        }
      }
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      document.save(outputStream);
      document.close();
      return new BufferedInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    } catch (IOException e) {
      log.warn("PDF compression for file '{}' skipped due to: {}. Original data preserved.", fileName, e.getMessage());
      return inputStream;
    }
  }

  /**
   * Checks if the given filename represents a PDF file that can be compressed.
   *
   * @param filename    the name of the file to check, cannot be null
   * @param inputStream the input stream of the file to be compressed, cannot be null
   * @return true if the file is a PDF, false otherwise
   */
  @Override
  public boolean canCompress(@NonNull String filename, @NonNull long fileSize, @NonNull BufferedInputStream inputStream) {
    if (minCompressibleFileSize.toBytes() > fileSize) {
      return false;
    }
    var metadata = new Metadata();
    metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
    try {
      var mediaType = defaultDetector.detect(inputStream, metadata);
      return SUPPORTED_TYPES.contains(mediaType);
    } catch (IOException e) {
      throw new FileCompressionException("Failed to detect PDF file", e);
    }
  }

  private Map<ImageEntry, List<PDResources>> collectImagesFromPdf(PDDocument document) throws IOException {
    Map<ImageEntry, List<PDResources>> images = new LinkedHashMap<>();
    for (PDPage page : document.getPages()) {
      PDResources resources = page.getResources();
      // Skip if no resources or XObjects
      if (resources == null) {
        continue;
      }
      for (COSName name : resources.getXObjectNames()) {
        if (resources.getXObject(name) instanceof PDImageXObject) {
          var image = (PDImageXObject) resources.getXObject(name);
          var entry = new ImageEntry(name, image);
          if (images.containsKey(entry)) {
            images.get(entry).add(resources);
          } else {
            images.put(entry, new ArrayList<>(List.of(resources)));
          }
        }
      }
    }
    return images;
  }

  private ImageEntry compressImageEntry(
    PDDocument document, ImageEntry entry, int imageMaxWidth, int imageMaxHeight, int compressionQuality) {
    try {
      BufferedImage inputImage = entry.getImage().getImage();
      boolean hasAlpha = inputImage.getColorModel().hasAlpha();

      ImageProcessingUtils.ImageDimensions dims =
        ImageProcessingUtils.calculateNewImageDimensions(inputImage, imageMaxWidth, imageMaxHeight);

      BufferedImage resized = ImageProcessingUtils.resizeImage(
        inputImage, dims, hasAlpha);

      float qualityFactor = compressionQuality / 100f;

      // Use appropriate format based on alpha channel
      byte[] imageBytes;
      if (!hasAlpha) {
        imageBytes = ImageProcessingUtils.writeImageAsJpeg(resized, qualityFactor);
      } else {
        imageBytes = ImageProcessingUtils.writeImageAsPng(resized);
      }

      PDImageXObject newImage = PDImageXObject.createFromByteArray(
        document, imageBytes, entry.getCOSName().getName());
      return new ImageEntry(entry.getCOSName(), newImage);
    } catch (IOException e) {
      throw new FileCompressionException("Failed to compress PDF file", e);
    }
  }
}
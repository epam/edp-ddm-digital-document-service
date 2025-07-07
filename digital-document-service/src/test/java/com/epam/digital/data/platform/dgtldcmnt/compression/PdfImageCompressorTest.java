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
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfImageCompressorTest {

  private static final String FILE_NAME = "file.pdf";

  private static final int DEFAULT_IMAGE_MAX_WIDTH = 800;
  private static final int DEFAULT_IMAGE_MAX_HEIGHT = 600;
  private static final int DEFAULT_COMPRESSION_QUALITY = 80;
  private static final DataSize DEFAULT_MIN_COMPRESSIBLE_FILE_SIZE = DataSize.ofBytes(5);

  private PdfImageCompressor compressor;

  @Mock
  private Detector defaultDetector;

  @Mock
  private StandardImageCompressor standardImageCompressor;

  @BeforeEach
  void setUp() throws IOException {
    compressor = new PdfImageCompressor(
        defaultDetector,
        DEFAULT_IMAGE_MAX_WIDTH,
        DEFAULT_IMAGE_MAX_HEIGHT,
        DEFAULT_COMPRESSION_QUALITY,
        DEFAULT_MIN_COMPRESSIBLE_FILE_SIZE);

    // Configure mock behavior for detector to detect PDF files
    lenient().when(defaultDetector.detect(any(InputStream.class), any(Metadata.class)))
        .thenReturn(MediaType.parse("application/pdf"));

    // Configure mock behavior for imageCompressor to pass through the input stream
    lenient().when(standardImageCompressor.compress(any(String.class), any(BufferedInputStream.class), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));
  }

  @Test
  @DisplayName("Should successfully compress PDF with embedded images")
  void testCompressPdfWithEmbeddedImages() throws IOException {
    // Create a test PDF with an embedded image
    int originalWidth = 1000;
    int originalHeight = 800;
    var pdfData = createPdfWithImage(originalWidth, originalHeight);
    var pdfInput = new BufferedInputStream(new ByteArrayInputStream(pdfData));

    // Compress the PDF
    var compressedPdf = compressor.compress(FILE_NAME, pdfInput, ImageCompressorParameters.builder().build());

    // Verify the result is not null
    assertNotNull(compressedPdf);

    // Load the compressed PDF to verify
    try (PDDocument compressedDoc = PDDocument.load(compressedPdf)) {
      // Verify document was properly created
      assertNotNull(compressedDoc);
      assertTrue(compressedDoc.getNumberOfPages() > 0);

      // Extract and verify the compressed image dimensions
      List<BufferedImage> extractedImages = extractImagesFromPdf(compressedDoc);
      assertFalse(extractedImages.isEmpty(), "No images found in compressed PDF");

      BufferedImage compressedImage = extractedImages.get(0);
      assertNotNull(compressedImage);
    }
  }

  @Test
  @DisplayName("Should compress PDF with custom parameters")
  void testCompressWithCustomParameters() throws IOException {
    // Create a test PDF with an embedded image
    int originalWidth = 1200;
    int originalHeight = 1000;
    var pdfData = createPdfWithImage(originalWidth, originalHeight);
    var pdfInput = new BufferedInputStream(new ByteArrayInputStream(pdfData));

    // Custom dimensions for testing
    int customMaxWidth = 360;
    int customMaxHeight = 300;
    int customQuality = 50;

    // Create custom parameters
    ImageCompressorParameters parameters = ImageCompressorParameters.builder()
        .imageMaxWidth(customMaxWidth)
        .imageMaxHeight(customMaxHeight)
        .compressionQuality(customQuality)
        .build();

    // Compress the PDF with custom parameters
    var compressedPdf = compressor.compress(FILE_NAME, pdfInput, parameters);

    // Verify the result is not null
    assertNotNull(compressedPdf);

    // Load the compressed PDF to verify
    try (PDDocument compressedDoc = PDDocument.load(compressedPdf)) {
      // Verify document was properly created
      assertNotNull(compressedDoc);
      assertTrue(compressedDoc.getNumberOfPages() > 0);

      // Extract and verify the compressed image dimensions
      List<BufferedImage> extractedImages = extractImagesFromPdf(compressedDoc);
      assertFalse(extractedImages.isEmpty(), "No images found in compressed PDF");
    }
  }

  @Test
  @DisplayName("Should handle PDF without embedded images")
  void testCompressPdfWithoutImages() throws IOException {
    // Create a simple PDF without images
    var pdfData = createEmptyPdf();
    var pdfInput = new BufferedInputStream(new ByteArrayInputStream(pdfData));

    // Compress the PDF
    var compressedPdf = compressor.compress(FILE_NAME, pdfInput, ImageCompressorParameters.builder().build());

    // Verify the result is not null
    assertNotNull(compressedPdf);

    // Load the compressed PDF to verify
    try (PDDocument compressedDoc = PDDocument.load(compressedPdf)) {
      // Verify document was properly created
      assertNotNull(compressedDoc);
      assertEquals(1, compressedDoc.getNumberOfPages());

      // Verify no images in the document
      List<BufferedImage> extractedImages = extractImagesFromPdf(compressedDoc);
      assertTrue(extractedImages.isEmpty(), "No images should be present in the PDF");
    }
  }

  @Test
  @DisplayName("Should handle PDF with multiple pages and images")
  void testCompressPdfWithMultiplePages() throws IOException {
    // Create a multi-page PDF with images
    int pageCount = 3;
    var pdfData = createMultiPagePdfWithImages(pageCount);
    var pdfInput = new BufferedInputStream(new ByteArrayInputStream(pdfData));

    // Compress the PDF
    var compressedPdf = compressor.compress(FILE_NAME, pdfInput, ImageCompressorParameters.builder().build());

    // Verify the result is not null
    assertNotNull(compressedPdf);

    // Load the compressed PDF to verify
    try (PDDocument compressedDoc = PDDocument.load(compressedPdf)) {
      // Verify document was properly created
      assertNotNull(compressedDoc);
      assertEquals(pageCount, compressedDoc.getNumberOfPages());

      // Extract and verify the compressed images
      List<BufferedImage> extractedImages = extractImagesFromPdf(compressedDoc);
      assertEquals(pageCount, extractedImages.size(),
          "Should have one image per page in the compressed PDF");
    }
  }

  @Test
  @DisplayName("Should maintain visual content when compressing")
  void testImageContentPreservedAfterCompression() throws IOException {
    // Create a PDF with a distinctive pattern image
    var pdfData = createPdfWithPatternImage();
    var pdfInput = new BufferedInputStream(new ByteArrayInputStream(pdfData));

    // Compress the PDF
    var compressedPdf = compressor.compress(FILE_NAME, pdfInput, ImageCompressorParameters.builder().build());

    // Load the compressed PDF to verify
    try (PDDocument compressedDoc = PDDocument.load(compressedPdf)) {
      // Extract the compressed image
      List<BufferedImage> extractedImages = extractImagesFromPdf(compressedDoc);
      assertFalse(extractedImages.isEmpty(), "No images found in compressed PDF");

      BufferedImage compressedImage = extractedImages.get(0);
      assertNotNull(compressedImage);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"document.pdf", "report.PDF", "file.pDf"})
  @DisplayName("Should return true for valid PDF file extensions")
  void testCanCompressWithValidExtensions(String filename) throws IOException {
    // Create a mock input stream
    int fileSize = 10;
    var inputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[fileSize]));

    // Configure mock to return application/pdf media type
    when(defaultDetector.detect(any(), any())).thenReturn(MediaType.parse("application/pdf"));

    assertTrue(compressor.canCompress(filename, fileSize, inputStream));
  }

  @ParameterizedTest
  @ValueSource(strings = {"document.jpg", "report.docx", "file.txt", "document", ".pdf.txt"})
  @DisplayName("Should return false for non-PDF file extensions")
  void testCanCompressWithInvalidExtensions(String filename) throws IOException {
    // Create a mock input stream
    var inputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
    int fileSize = 10;

    // Configure mock to return non-PDF media type
    String mediaType;
    if (filename.endsWith(".jpg")) {
      mediaType = "image/jpeg";
    } else if (filename.endsWith(".docx")) {
      mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    } else if (filename.endsWith(".txt")) {
      mediaType = "text/plain";
    } else {
      mediaType = "application/octet-stream";
    }

    when(defaultDetector.detect(any(), any())).thenReturn(MediaType.parse(mediaType));

    assertFalse(compressor.canCompress(filename,fileSize, inputStream));
  }

  /**
   * Helper method to create a test PDF with a single embedded image.
   */
  private byte[] createPdfWithImage(int imageWidth, int imageHeight) throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);

    // Create a simple image
    BufferedImage bufferedImage = createTestImage(imageWidth, imageHeight);

    // Convert the image to a PDImageXObject
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "JPEG", baos);
    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
        document, baos.toByteArray(), "test-image.jpg");

    // Draw the image on the page
    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
      contentStream.drawImage(pdImage, 0, 0);
    }

    // Save the document to a byte array
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    document.save(outputStream);
    document.close();

    return outputStream.toByteArray();
  }

  /**
   * Creates a test image with specified dimensions.
   */
  private BufferedImage createTestImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(Color.BLACK);
    graphics.drawRect(10, 10, width - 20, height - 20);
    graphics.dispose();
    return image;
  }

  /**
   * Creates a PDF with an image containing a distinctive pattern for visual testing.
   */
  private byte[] createPdfWithPatternImage() throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);

    // Create a test image with a distinctive pattern
    BufferedImage bufferedImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = bufferedImage.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, 600, 600);

    // Draw a distinctive pattern
    graphics.setColor(Color.BLACK);

    // Draw a cross in the middle
    int centerX = 300;
    int centerY = 300;
    graphics.drawLine(centerX - 100, centerY, centerX + 100, centerY);
    graphics.drawLine(centerX, centerY - 100, centerX, centerY + 100);

    // Draw a circle
    graphics.drawOval(centerX - 50, centerY - 50, 100, 100);

    // Draw some text
    graphics.drawString("Compression Test", centerX - 50, centerY + 150);

    graphics.dispose();

    // Convert the image to a PDImageXObject
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "JPEG", baos);
    PDImageXObject pdImage = PDImageXObject.createFromByteArray(
        document, baos.toByteArray(), "pattern-image");

    // Draw the image on the page
    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
      contentStream.drawImage(pdImage, 0, 0);
    }

    // Save the document to a byte array
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    document.save(outputStream);
    document.close();

    return outputStream.toByteArray();
  }

  /**
   * Helper method to create a simple empty PDF.
   */
  private byte[] createEmptyPdf() throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    document.save(outputStream);
    document.close();

    return outputStream.toByteArray();
  }

  /**
   * Helper method to create a PDF with multiple pages, each with an embedded image.
   */
  private byte[] createMultiPagePdfWithImages(int pageCount) throws IOException {
    PDDocument document = new PDDocument();

    for (int i = 0; i < pageCount; i++) {
      PDPage page = new PDPage();
      document.addPage(page);

      // Create a simple image
      BufferedImage bufferedImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = bufferedImage.createGraphics();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, 800, 600);
      graphics.setColor(Color.BLACK);
      graphics.drawString("Page " + (i + 1), 100, 100);
      graphics.dispose();

      // Convert the image to a PDImageXObject
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "JPEG", baos);
      PDImageXObject pdImage = PDImageXObject.createFromByteArray(
          document, baos.toByteArray(), "test-image-page-" + (i + 1));

      // Draw the image on the page
      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        contentStream.drawImage(pdImage, 0, 0);
      }
    }

    // Save the document to a byte array
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    document.save(outputStream);
    document.close();

    return outputStream.toByteArray();
  }

  /**
   * Extracts all images from a PDF document.
   *
   * @param document The PDF document to extract images from
   * @return List of extracted images as BufferedImage objects
   * @throws IOException If there is an error processing the PDF
   */
  private List<BufferedImage> extractImagesFromPdf(PDDocument document) throws IOException {
    List<BufferedImage> extractedImages = new ArrayList<>();

    for (PDPage page : document.getPages()) {
      PDResources resources = page.getResources();
      if (resources != null) {
        for (COSName name : resources.getXObjectNames()) {
          if (resources.isImageXObject(name)) {
            PDImageXObject image = (PDImageXObject) resources.getXObject(name);
            extractedImages.add(image.getImage());
          }
        }
      }
    }

    return extractedImages;
  }

  /**
   * Converts a BufferedImage to a byte array for size comparison.
   */
  private byte[] convertImageToBytes(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "JPEG", baos);
    return baos.toByteArray();
  }
}
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

package com.epam.digital.data.platform.dgtldcmnt.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@UtilityClass
public class ImageProcessingUtils {
  /**
   * Determines if an image needs alpha channel support (PNG) vs can be JPEG.
   * Checks both ColorModel and BufferedImage type for robustness.
   */
  public boolean requiresAlphaChannel(BufferedImage image) {
    if (image.getColorModel().hasAlpha()) {
      return true;
    }
    
    int type = image.getType();
    return type == BufferedImage.TYPE_4BYTE_ABGR 
        || type == BufferedImage.TYPE_INT_ARGB
        || type == BufferedImage.TYPE_INT_ARGB_PRE;
  }

  public ImageDimensions calculateNewImageDimensions(BufferedImage inputImage,
                                                            int imageMaxWidth,
                                                            int imageMaxHeight) {
    int originalWidth = inputImage.getWidth();
    int originalHeight = inputImage.getHeight();

    if ((imageMaxWidth == 0 || originalWidth <= imageMaxWidth)
        && (imageMaxHeight == 0 || originalHeight <= imageMaxHeight)) {
      return new ImageDimensions(originalWidth, originalHeight);
    }

    double aspectRatio = (double) originalWidth / originalHeight;
    int newWidth = originalWidth;
    int newHeight = originalHeight;

    if (imageMaxWidth > 0 && originalWidth > imageMaxWidth) {
      newWidth = imageMaxWidth;
      newHeight = (int) (newWidth / aspectRatio);
    }

    if (imageMaxHeight > 0 && newHeight > imageMaxHeight) {
      newHeight = imageMaxHeight;
      newWidth = (int) (newHeight * aspectRatio);
    }

    return new ImageDimensions(newWidth, newHeight);
  }

  public BufferedImage resizeImage(BufferedImage inputImage, ImageDimensions imageDimensions, boolean preserveAlpha) {
    int imageType;
    
    if (preserveAlpha) {
      // Preserve alpha channel
      imageType = BufferedImage.TYPE_INT_ARGB;
    } else {
      // RGB without alpha (for JPEG)
      imageType = BufferedImage.TYPE_INT_RGB;
    }

    int newWidth = imageDimensions.getWidth();
    int newHeight = imageDimensions.getHeight();
    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, imageType);
    Graphics2D g2d = resizedImage.createGraphics();
    try {
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      if (!preserveAlpha) {
        // Set white background for JPEG to avoid black artifacts
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, newWidth, newHeight);
      }
      g2d.drawImage(inputImage, 0, 0, newWidth, newHeight, null);
    } finally {
      g2d.dispose();
    }
    return resizedImage;
  }

  public byte[] writeImageAsJpeg(BufferedImage image, float quality) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IllegalStateException("No JPEG writers available");
    }
    ImageWriter writer = writers.next();
    try (ImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
      writer.setOutput(ios);
      ImageWriteParam param = writer.getDefaultWriteParam();
      if (quality > 0 && param.canWriteCompressed()) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
      }
      writer.write(null, new IIOImage(image, null, null), param);
      ios.flush();
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }

  public byte[] writeImageAsPng(BufferedImage image) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    boolean written = ImageIO.write(image, "png", outputStream);
    if (!written) {
      throw new IOException("Failed to write image as PNG");
    }
    return outputStream.toByteArray();
  }

  @Getter
  @RequiredArgsConstructor
  public static class ImageDimensions {
    private final int width;
    private final int height;
  }
}

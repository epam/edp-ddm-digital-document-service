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
import org.springframework.lang.NonNull;

import java.io.BufferedInputStream;

/**
 * Interface that defines the operations for file compression.
 * Provides a consistent interface for all image compressor implementations.
 */
public interface ImageCompressor {

  /**
   * Compresses a file from the given input stream and returns the compressed data as an input stream
   * using provided compression parameters.
   *
   * @param filename    the name of the file to be compressed, cannot be null
   * @param inputStream the input stream containing the data to be compressed, cannot be null
   * @param parameters  compression parameters that control the compression process, cannot be null
   * @return an input stream containing the compressed data
   * @throws FileCompressionException if any error occurs during compression
   */
  @NonNull
  BufferedInputStream compress(@NonNull String filename, @NonNull BufferedInputStream inputStream, 
      @NonNull ImageCompressorParameters parameters) throws FileCompressionException;

  /**
   * Validates if a file with the given filename can be compressed by this compressor.
   *
   * @param filename    the name of the file to be compressed, cannot be null
   * @param inputStream the input stream of the file to be compressed, cannot be null
   * @return true if the file can be compressed by this compressor, false otherwise
   */
  boolean canCompress(@NonNull String filename, @NonNull long fileSize, @NonNull BufferedInputStream inputStream);

  int getImageMaxWidth();

  int getImageMaxHeight();

  int getCompressionQuality();
}
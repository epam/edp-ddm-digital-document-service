/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.dgtldcmnt.service;

import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadRemoteDocumentDto;

/**
 * The service for management of the documents.
 */
public interface InternalApiDocumentService {

  /**
   * This method downloads document from remote storage and put it to local storage. It generates 
   * hash and document id. Document id and file name are stored in the document metadata. 
   * The hash is SHA-256 digest and returns the value as a hex string. The document id(key) 
   * is pseudo randomly generated UUID(type 4).
   * This method does not store the whole document in memory, but only a small buffer, 
   * which allows you to upload large files without fear of getting an OutOfMemoryError
   *
   * @param documentDto contains file input stream, metadata, and document context info.
   */
  RemoteDocumentMetadataDto put(UploadRemoteDocumentDto documentDto);
}

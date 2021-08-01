package com.epam.digital.data.platform.dgtldcmnt.service;

import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.exception.DocumentNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * The service for management of the documents.
 */
public interface DocumentService {

  /**
   * Put document to storage. It generates hash and document id that are stored in the document
   * metadata. The hash is SHA-256 digest and returns the value as a hex string. The document
   * id(key) is pseudo randomly generated UUID(type 4).
   *
   * @param uploadDocumentDto contains file input stream, metadata, and document context info.
   * @return {@link DocumentMetadataDto} of the saved document.
   */
  DocumentMetadataDto put(UploadDocumentDto uploadDocumentDto);

  /**
   * Get document from storage by key. It returns document representation with document name,
   * content-type, size and content as {@link InputStream} (for further downloading).
   *
   * @param getDocumentDto contains document id and the document context info.
   * @return document representation.
   * @throws DocumentNotFoundException if document not exist.
   */
  DocumentDto get(GetDocumentDto getDocumentDto);

  /**
   * Get documents metadata by keys.
   *
   * @param getMetadataDto contains document ids and a context of the documents.
   * @return list of documents metadata.
   * @throws DocumentNotFoundException if at least one document not exist in provided ids list.
   */
  List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto);
}

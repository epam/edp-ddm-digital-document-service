package com.epam.digital.data.platform.dgtldcmnt.util;

import org.springframework.stereotype.Component;

@Component
public class CephKeyProvider {

  public static final String DOCUMENT_ID_FORMAT = "process/%s/%s";

  public String generateKey(String documentId, String processInstanceId) {
    return String.format(DOCUMENT_ID_FORMAT, processInstanceId, documentId);
  }
}

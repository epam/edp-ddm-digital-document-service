package com.epam.digital.data.platform.dgtldcmnt.exception;

import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * An exception that is thrown in case when at least one or more documents are not found.
 */
@Getter
public class DocumentNotFoundException extends RuntimeException {

  private final Collection<String> ids;

  public DocumentNotFoundException(Collection<String> ids) {
    super(String.format("At least one or more documents was not found in provided ids list %s", ids));
    this.ids = ids;
  }
}

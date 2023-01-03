package com.epam.digital.data.platform.dgtldcmnt.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import org.junit.jupiter.api.Test;

class RemoteFileSizeValidatorTest {

  private final RemoteFileSizeValidator instance = new RemoteFileSizeValidator(1);

  @Test
  void shouldNotThrowException() {
    assertDoesNotThrow(() -> instance.validate(1024 * 1024));
  }

  @Test
  void shouldThrowException() {
    assertThrows(ValidationException.class, () -> instance.validate(1024 * 1024 + 1));
  }
}
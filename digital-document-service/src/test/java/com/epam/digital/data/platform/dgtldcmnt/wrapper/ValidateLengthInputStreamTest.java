package com.epam.digital.data.platform.dgtldcmnt.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.epam.digital.data.platform.dgtldcmnt.validator.FileSizeValidator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidateLengthInputStreamTest {

  @Mock
  FileSizeValidator validator;
  
  private ValidateLengthInputStream instance;
  
  @BeforeEach
  void beforeEach() {
    var inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
    instance = new ValidateLengthInputStream(inputStream, validator);
  }
  
  @Test
  void shouldCallValidatorAfterEachByte() throws IOException {
    assertThat(instance.read()).isEqualTo(1);
    verify(validator).validate(1L);
    
    assertThat(instance.read()).isEqualTo(2);
    verify(validator).validate(2L);
    
    assertThat(instance.read()).isEqualTo(3);
    verify(validator).validate(3L);
    
    assertThat(instance.read()).isEqualTo(-1);
    verify(validator, times(2)).validate(3L);
  }

  @Test
  void shouldCallValidatorWithCorrectNumberOfBytesForReatMethod() throws IOException {
    instance.read(new byte[2]);
    verify(validator).validate(2L);
  }

  @Test
  void shouldCallValidatorWithNumberOfBytesThatEqLength() throws IOException {
    instance.read(new byte[10]);
    verify(validator).validate(3L);
  }
  
  @Test
  void shouldCallValidatorWithCorrectNumberOfBytesForSkipMethod() throws IOException {
    instance.skip(instance.available());
    verify(validator).validate(3L);
  }

  @Test
  void shouldCallValidatorWithNumberOfBytesThatEqRemaining() throws IOException {
    instance.skip(10L);
    verify(validator).validate(3L);
  }

  @Test
  void shouldCallValidatorWithCorrectValueWhenUseMarkAndReset() throws IOException {
    assertThat(instance.read()).isEqualTo(1);
    verify(validator).validate(1L);
    instance.mark(2);

    assertThat(instance.read()).isEqualTo(2);
    verify(validator).validate(2L);

    assertThat(instance.read()).isEqualTo(3);
    verify(validator).validate(3L);
    
    instance.reset();
    assertThat(instance.read()).isEqualTo(2);
    verify(validator, times(2)).validate(2L);

    assertThat(instance.read()).isEqualTo(3);
    verify(validator, times(2)).validate(3L);
    
    assertThat(instance.read()).isEqualTo(-1);
    verify(validator, times(3)).validate(3L);
  }
}
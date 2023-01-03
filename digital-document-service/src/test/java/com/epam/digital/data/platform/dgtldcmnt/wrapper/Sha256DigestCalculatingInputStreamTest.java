package com.epam.digital.data.platform.dgtldcmnt.wrapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sha256DigestCalculatingInputStreamTest {

  private static final String INITIAL_CHECKSUM =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  private static final String EXPECTED_CHECKSUM =
      "039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81";
  private Sha256DigestCalculatingInputStream instance;


  @BeforeEach
  void beforeEach() {
    var inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
    instance = new Sha256DigestCalculatingInputStream(inputStream);
  }

  @Test
  void shouldCorrectlyCountChecksumWhenReadOneByOneByte() throws IOException {
    String initialChecksum = Hex.encodeHexString(instance.getDigest());
    instance.read();
    instance.read();
    instance.read();
    String finalChecksum = Hex.encodeHexString(instance.getDigest());

    assertThat(initialChecksum).isEqualTo(INITIAL_CHECKSUM);
    assertThat(finalChecksum).isEqualTo(EXPECTED_CHECKSUM);
  }


  @Test
  void shouldResetChecksumToInitialValueIfCallGetDigestMoreThenOnce() throws IOException {
    instance.read();
    instance.read();
    instance.read();

    String finalChecksum = Hex.encodeHexString(instance.getDigest());
    assertThat(finalChecksum).isEqualTo(EXPECTED_CHECKSUM);

    finalChecksum = Hex.encodeHexString(instance.getDigest());
    assertThat(finalChecksum).isEqualTo(INITIAL_CHECKSUM);
  }

  @Test
  void shouldCorrectlyCountChecksumWhenReadUsingByteBuffer() throws IOException {
    String initialChecksum = Hex.encodeHexString(instance.getDigest());
    instance.read();
    instance.read(new byte[1]);
    instance.read(new byte[instance.available()]);
    String finalChecksum = Hex.encodeHexString(instance.getDigest());

    assertThat(initialChecksum).isEqualTo(INITIAL_CHECKSUM);
    assertThat(finalChecksum).isEqualTo(EXPECTED_CHECKSUM);
  }

  @Test
  void shouldCorrectlyCountChecksumWhenUsedMarkAndReset() throws IOException {
    instance.read();
    instance.mark(2);
    instance.read();
    instance.reset();
    instance.read();
    instance.read();

    String finalChecksum = Hex.encodeHexString(instance.getDigest());
    assertThat(finalChecksum).isEqualTo(EXPECTED_CHECKSUM);
  }
}
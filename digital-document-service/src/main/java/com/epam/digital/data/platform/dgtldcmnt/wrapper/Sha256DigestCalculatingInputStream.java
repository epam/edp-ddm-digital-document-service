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

package com.epam.digital.data.platform.dgtldcmnt.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple InputStream wrapper that examines the wrapped stream's contents as they are read and
 * calculates and SHA256 digest.
 */
@NotThreadSafe
public class Sha256DigestCalculatingInputStream extends InputStream {

  private static final Log log = LogFactory.getLog(Sha256DigestCalculatingInputStream.class);

  private final InputStream is;

  // The SHA256 message digest being calculated by this input stream
  private MessageDigest digest;

  private final boolean digestCanBeCloned;

  // The SHA256 message digest as at the time when the last mark(int) operation was called; 
  // always null if mark is not supported.
  private MessageDigest digestLastMarked;

  public Sha256DigestCalculatingInputStream(InputStream is) {
    this.is = is;
    digest = DigestUtils.getSha256Digest();
    digestCanBeCloned = canBeCloned(digest);
    if (is.markSupported() && !digestCanBeCloned) {
      log.debug(
          "Mark-and-reset disabled on SHA256 calculation because the digest implementation does not support cloning. "
              + "This will limit the SDK's ability to retry requests that failed. Consider pre-calculating the SHA256 "
              + "checksum for the request or switching to a security provider that supports message digest cloning.");
    }
  }

  private boolean canBeCloned(MessageDigest digest) {
    try {
      digest.clone();
      return true;
    } catch (CloneNotSupportedException e) {
      return false;
    }
  }

  private MessageDigest cloneFrom(MessageDigest from) {
    try {
      return (MessageDigest) from.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Message digest implementation does not support cloning.", e);
    }
  }

  public byte[] getDigest() {
    return digest.digest();
  }

  @Override
  public int available() throws IOException {
    return is.available();
  }

  @Override
  public boolean markSupported() {
    // Cloning of the digest is required to support restoring the prior state of the SHA256 calculation when using mark() and
    // reset(). If the digest doesn't support cloning, we have to disable mark/reset support.
    return is.markSupported() && digestCanBeCloned;
  }

  public int read() throws IOException {
    int ch = is.read();
    if (ch != -1) {
      digest.update((byte) ch);
    }
    return ch;
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    int ch = is.read(b, off, len);
    if (ch != -1) {
      digest.update(b, off, ch);
    }
    return ch;
  }

  @Override
  public long skip(long skipped) throws IOException {
    return is.skip(skipped);
  }

  @Override
  public synchronized void mark(int readlimit) {
    if (markSupported()) {
      is.mark(readlimit);
      digestLastMarked = cloneFrom(digest);
    }
  }

  @Override
  public synchronized void reset() throws IOException {
    if (markSupported()) {
      is.reset();

      if (digestLastMarked == null) {
        digest = DigestUtils.getSha256Digest();
      } else {
        digest = cloneFrom(digestLastMarked);
      }
    } else {
      throw new IOException("mark/reset not supported");
    }
  }

  @Override
  public synchronized void close() throws IOException {
    is.close();
  }
}

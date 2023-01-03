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

import com.epam.digital.data.platform.dgtldcmnt.validator.FileSizeValidator;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotNull;

/**
 * count the number of bytes read through the stream
 */
public class ValidateLengthInputStream extends InputStream {

  private long count = 0;
  private long marked = -1;
  private final InputStream is;
  private final FileSizeValidator validator;
  
  public ValidateLengthInputStream(InputStream is, FileSizeValidator validator) {
    this.is = is;
    this.validator = validator;
  }

  @Override
  public int available() throws IOException {
    return is.available();
  }

  @Override
  public boolean markSupported() {
    return is.markSupported();
  }

  public int read() throws IOException {
    int r = is.read();
    if (r > 0) {
      count++;
    }
    validator.validate(count);
    return r;
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    int r = is.read(b, off, len);
    if (r > 0) {
      count += r;
    }
    validator.validate(count);
    return r;
  }

  @Override
  public long skip(long skipped) throws IOException {
    long l = is.skip(skipped);
    if (l > 0) {
      count += l;
    }
    validator.validate(count);
    return l;
  }

  @Override
  public synchronized void mark(int readlimit) {
    is.mark(readlimit);
    marked = count;
  }

  @Override
  public synchronized void reset() throws IOException {
    is.reset();
    count = marked;
  }

  @Override
  public synchronized void close() throws IOException {
    is.close();
  }
}
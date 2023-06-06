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

package com.epam.digital.data.platform.dgtldcmnt.util.unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@RequiredArgsConstructor
public class FractionalDataSize {

  private static final Pattern PATTERN = Pattern.compile("^(\\d+\\.?\\d*)([a-zA-Z]{0,2})$");
  private static final BigDecimal BYTES_IN_MEGABYTE = BigDecimal.valueOf(
      DataSize.ofMegabytes(1).toBytes());

  private final long bytes;

  public static FractionalDataSize parse(@NonNull String text) {
    var matcher = PATTERN.matcher(StringUtils.trimAllWhitespace(text));
    Assert.state(matcher.matches(), "Does not match data size pattern");
    var unit = DataUnit.fromSuffix(matcher.group(2));
    var bytesInUnit = BigDecimal.valueOf(DataSize.of(1L, unit).toBytes());
    var amountUnits = BigDecimal.valueOf(Double.parseDouble(matcher.group(1)));
    return new FractionalDataSize(amountUnits.multiply(bytesInUnit).longValue());
  }

  public long toBytes() {
    return this.bytes;
  }

  public double toMegabytes() {
    return BigDecimal.valueOf(bytes).divide(BYTES_IN_MEGABYTE, 2, RoundingMode.HALF_UP)
        .doubleValue();
  }
}

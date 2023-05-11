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

package com.epam.digital.data.platform.dgtldcmnt.config;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class TestS3ObjectCephService implements CephService {

  private final Map<String, S3Object> storage = new HashMap<>();

  @Override
  @SneakyThrows
  public CephObjectMetadata put(String cephBucketName, String key, String contentType,
      Map<String, String> userMetadata, InputStream fileInputStream) {
    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentType(contentType);
    objectMetadata.setContentLength(fileInputStream.available());
    objectMetadata.setUserMetadata(userMetadata);
    S3Object s3Object = new S3Object();
    s3Object.setObjectContent(fileInputStream);
    s3Object.setObjectMetadata(objectMetadata);
    storage.put(key, s3Object);
    return toCephObjectMetadata(s3Object.getObjectMetadata());
  }

  @SneakyThrows
  @Override
  public CephObjectMetadata put(String cephBucketName, String key, String contentType,
      long contentLength, Map<String, String> userMetadata, InputStream fileInputStream) {
    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentType(contentType);
    objectMetadata.setContentLength(contentLength);
    objectMetadata.setUserMetadata(userMetadata);
    S3Object s3Object = new S3Object();
    s3Object.setObjectContent(fileInputStream);
    s3Object.setObjectMetadata(objectMetadata);
    storage.put(key, s3Object);
    fileInputStream.read(new byte[fileInputStream.available()]);
    return toCephObjectMetadata(s3Object.getObjectMetadata());
  }

  @Override
  public Optional<CephObject> get(String cephBucketName, String key) {
    S3Object s3Object = storage.get(key);
    if (Objects.isNull(s3Object)) {
      return Optional.empty();
    }
    return Optional.of(tpCephObject(s3Object));
  }

  @Override
  public Optional<String> getAsString(String s, String s1) {
    return Optional.empty();
  }

  @Override
  public void put(String cephBucketName, String key, String content) {
  }

  @Override
  public List<CephObjectMetadata> getMetadata(String cephBucketName, Set<String> keys) {
    boolean allContains = keys.stream().allMatch(storage::containsKey);
    if (!allContains) {
      return Collections.emptyList();
    }
    var objectMetadata = keys.stream().map(k -> storage.get(k).getObjectMetadata())
        .collect(Collectors.toList());
    return toCephObjectMetadataList(objectMetadata);
  }

  @Override
  public List<CephObjectMetadata> getMetadata(String cephBucketName, String prefix) {
    var keys = storage.keySet().stream().filter(k -> k.startsWith(prefix))
        .collect(Collectors.toList());
    var objectMetadata = keys.stream().map(k -> storage.get(k).getObjectMetadata())
        .collect(Collectors.toList());
    return toCephObjectMetadataList(objectMetadata);
  }

  @Override
  public CephObjectMetadata setUserMetadata(String cephBucketName, String key,
      Map<String, String> userMetadata) {
    var s3Object = storage.containsKey(key) ? storage.get(key) : new S3Object();
    ObjectMetadata objectMetadata = Objects.isNull(s3Object.getObjectMetadata()) ? new ObjectMetadata()
            : s3Object.getObjectMetadata();
    objectMetadata.setUserMetadata(userMetadata);
    s3Object.setObjectMetadata(objectMetadata);
    storage.put(key, s3Object);
    return toCephObjectMetadata(s3Object.getObjectMetadata());
  }

  @Override
  public void delete(String cephBucketName, Set<String> keys) {
    keys.forEach(storage::remove);
  }

  @Override
  public Boolean exist(String cephBucketName, String key) {
    return Objects.nonNull(storage.get(key));
  }

  @Override
  public Boolean exist(String cephBucketName, Set<String> keys) {
    return keys.stream().allMatch(storage::containsKey);
  }

  @Override
  public Set<String> getKeys(String cephBucketName, String prefix) {
    return storage.keySet().stream().filter(k -> k.startsWith(prefix)).collect(Collectors.toSet());
  }

  @Override
  public Set<String> getKeys(String cephBucketName) {
    return new HashSet<>();
  }

  private List<CephObjectMetadata> toCephObjectMetadataList(
      List<ObjectMetadata> objectMetadataList) {
    return objectMetadataList.stream().map(this::toCephObjectMetadata).collect(Collectors.toList());
  }

  private CephObjectMetadata toCephObjectMetadata(ObjectMetadata objectMetadata) {
    return CephObjectMetadata.builder()
        .contentType(objectMetadata.getContentType())
        .userMetadata(objectMetadata.getUserMetadata())
        .contentLength(objectMetadata.getContentLength())
        .build();
  }

  private CephObject tpCephObject(S3Object s3Object) {
    return CephObject.builder()
        .metadata(toCephObjectMetadata(s3Object.getObjectMetadata()))
        .content(s3Object.getObjectContent())
        .build();
  }
}

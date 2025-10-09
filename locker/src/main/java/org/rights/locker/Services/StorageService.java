package org.rights.locker.Services;

import java.io.InputStream;


public interface StorageService {
    String putOriginal(String key, InputStream in, long size, String contentType, String sha256);
    String putHot(String key, InputStream in, long size, String contentType);
    String signedGet(String bucketKey, int seconds);
}
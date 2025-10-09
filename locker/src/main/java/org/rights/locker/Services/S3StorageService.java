package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.InputStream;


@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {
    private final S3Client s3;
    @Value("${app.s3.bucketOriginals}") String bucketOriginals;
    @Value("${app.s3.bucketHot}") String bucketHot;


    @Override
    public String putOriginal(String key, InputStream in, long size, String contentType, String sha256) {
        s3.putObject(PutObjectRequest.builder().bucket(bucketOriginals).key(key).contentType(contentType)
                .metadata(java.util.Map.of("sha256", sha256)).build(), RequestBody.fromInputStream(in, size));
        return key;
    }


    @Override
    public String putHot(String key, InputStream in, long size, String contentType) {
        s3.putObject(PutObjectRequest.builder().bucket(bucketHot).key(key).contentType(contentType).build(),
                RequestBody.fromInputStream(in, size));
        return key;
    }


    @Override
    public String signedGet(String bucketKey, int seconds) {
// TODO: return pre-signed URL
        return "signed-url-placeholder";
    }
}
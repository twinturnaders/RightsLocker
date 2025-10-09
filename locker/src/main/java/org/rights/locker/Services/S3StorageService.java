package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
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
    public String signedGet(String key, int seconds) {
        var get = GetObjectRequest.builder().bucket(bucketHot).key(key).build();
        var presign = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(seconds)).getObjectRequest(get).build());
        return presign.url().toString();
    }


    @Override
    public Map<String, Object> signedPut(String key, String bucket, int seconds, String contentType) {
        var req = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
        var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(seconds)).putObjectRequest(req).build());
        return Map.of("url", presigned.url().toString(), "method", "PUT", "headers", presigned.signedHeaders());
    }
}
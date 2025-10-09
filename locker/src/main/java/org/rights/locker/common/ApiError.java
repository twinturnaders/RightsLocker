package org.rights.locker.common;


import java.time.Instant;


public record ApiError(String path, int status, String error, String message, Instant timestamp) {
    public static ApiError of(String path, int status, String error, String message) {
        return new ApiError(path, status, error, message, Instant.now());
    }
}
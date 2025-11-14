package org.rights.locker.DTOs;

public record TokenResponse(String accessToken, String refreshToken, String userId) {

    public TokenResponse(String access, String refresh) {
        this(access, refresh, null);
    }


}

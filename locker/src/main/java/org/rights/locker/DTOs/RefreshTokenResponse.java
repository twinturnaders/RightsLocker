package org.rights.locker.DTOs;

public record RefreshTokenResponse(String refreshToken, String userId) {

    public RefreshTokenResponse(String refresh) {
        this(refresh, null);
    }


}

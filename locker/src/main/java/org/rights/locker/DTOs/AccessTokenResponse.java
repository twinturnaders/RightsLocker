package org.rights.locker.DTOs;

public record AccessTokenResponse(String accessToken, String userId) {

    public AccessTokenResponse(String access) {
        this(access, null);
    }


}

package org.rights.locker.Utilities;

import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {
    public static void setRefreshCookie(HttpServletResponse res, String token, int maxAgeSeconds){
        // HttpOnly; Secure; SameSite=Strict; Path=/api/auth
        String cookie = new StringBuilder()
                .append("refreshToken=").append(token).append(";")
                .append(" HttpOnly;")
                .append(" Secure;")
                .append(" SameSite=Strict;")
                .append(" Path=/api/auth;")
                .append(" Max-Age=").append(maxAgeSeconds)
                .toString();
        res.addHeader("Set-Cookie", cookie);
    }
    public static void clearRefreshCookie(HttpServletResponse res){
        res.addHeader("Set-Cookie", "refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0");
    }
}
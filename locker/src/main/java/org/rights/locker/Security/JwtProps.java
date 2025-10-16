package org.rights.locker.Security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProps(
        String issuer,
        String secret,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {}
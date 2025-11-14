package org.rights.locker.Security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated

public record JwtProps(
        // ③ These names map from kebab-case in YAML (e.g., access-ttl-seconds)
        @NotBlank String issuer,
        @NotBlank String secret,
        @Positive long accessTtlSeconds,
        @Positive long refreshTtlSeconds
) {}
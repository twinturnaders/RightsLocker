package org.rights.locker.Security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProps {
    private String secret;
    private long accessTtlSeconds;
    private long refreshTtlSeconds;

}
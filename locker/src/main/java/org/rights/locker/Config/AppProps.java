// src/main/java/org/rights/locker/Config/AppProps.java
package org.rights.locker.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter @Setter
@ConfigurationProperties(prefix = "app")
public class AppProps {
    /** processing | hosted (default processing) */
    private String mode = "processing";
    /** auto-delete window for processing mode, in hours (default 24) */
    private int retentionHours = 24;
    /** none|minimal|full (default minimal) */
    private String storeMetadata = "minimal";

    public static final class Attestation {
        @Getter @Setter private boolean enabled = true;
        @Getter @Setter private String issuer = "rightslocker.org";
        /** Optional HMAC secret to sign metadata.txt (can be empty to skip) */
        @Getter @Setter private String hmacSecret = "";
    }
    @Getter @Setter private Attestation attestation = new Attestation();
}

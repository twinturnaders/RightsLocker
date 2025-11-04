package org.rights.locker.DTOs;

import java.time.Instant;




public record ShareCreateRequest(
        Instant expiresAt,     // nullable -> default inside service (e.g., now+7d)
        boolean allowOriginal  // true = allow “original” download, else redacted-only
) {}
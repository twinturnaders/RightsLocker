package org.rights.locker.DTOs;

import java.time.Instant;


public record ShareCreateRequest(Instant expiresAt, boolean allowOriginal) { }
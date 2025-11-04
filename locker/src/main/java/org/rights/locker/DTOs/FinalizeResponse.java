package org.rights.locker.DTOs;

import org.rights.locker.Entities.Evidence;

public record FinalizeResponse(
        Evidence evidence,
        String shareToken   // null if not created
) {}
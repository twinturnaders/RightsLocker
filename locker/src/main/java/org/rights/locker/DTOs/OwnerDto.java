package org.rights.locker.DTOs;

import org.rights.locker.Enums.Role;

import java.util.UUID;

public record OwnerDto(
        UUID id,
        String email,
        String displayName,

        Role role
) {}
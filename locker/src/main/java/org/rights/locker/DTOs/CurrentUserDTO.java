package org.rights.locker.DTOs;

public record CurrentUserDTO(java.util.UUID id, String email, String passwordHash) {
}

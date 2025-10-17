package org.rights.locker.DTOs;

public record CurrentUser(Long id, String email, String passwordHash) {
}

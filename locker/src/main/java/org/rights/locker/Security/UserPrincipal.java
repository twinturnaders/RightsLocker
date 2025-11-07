package org.rights.locker.Security;

import lombok.Getter;
import lombok.Setter;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Role role;


    public UserPrincipal(UUID id, String email, String password,
                          Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }



    public static UserPrincipal create(AppUser user) {
        // Role enum already set on user; mapper ensures ROLE_ prefix
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole()
        );
    }

    @Override public String getUsername() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}


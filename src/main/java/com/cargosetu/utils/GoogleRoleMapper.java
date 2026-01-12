package com.cargosetu.utils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class GoogleRoleMapper implements GrantedAuthoritiesMapper {

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities) {

        List<GrantedAuthority> mapped = new ArrayList<>();

        // Give Google users SUPERADMIN (for now)
        mapped.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));

        return mapped;
    }
}


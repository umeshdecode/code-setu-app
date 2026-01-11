package com.cargosetu.config;

import com.cargosetu.config.dto.LoggedInUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtil;

    public JwtAuthenticationFilter(JwtUtils jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.validateToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                // Roles add ROLE_
                List<String> roles = claims.get("roles", List.class);
                Set<String> roleSet = new HashSet<>();
                if (roles != null) {
                    roles.forEach(r -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        roleSet.add(r); // store in LoggedInUser
                    });
                }

                // Permissions → added directly as authorities
                List<String> permissions = claims.get("permissions", List.class);
                Set<String> permissionSet = new HashSet<>();
                if (permissions != null) {
                    permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                    permissionSet.addAll(permissions);
                }

                LoggedInUser user = new LoggedInUser();
                user.setUserId(Integer.valueOf(claims.getSubject()));
                user.setUuid(claims.get("uuid", String.class));
                user.setEmail(claims.get("email", String.class));
                user.setUserName(claims.get("userName", String.class));
                user.setFirstName(claims.get("firstName", String.class));
                user.setLastName(claims.get("lastName", String.class));
                user.setRoles(roleSet);
                user.setPermissions(permissionSet);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

                System.out.println("LoggedIn user : "+user);
                System.out.println("Authenticated user: " + user.getUsername() +
                        ", Authorities: " + authorities);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                // Optional: log invalid token
                System.out.println("Invalid JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }



}

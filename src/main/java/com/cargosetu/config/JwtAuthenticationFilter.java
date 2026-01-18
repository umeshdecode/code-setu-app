package com.cargosetu.config;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.model.Permission;
import com.cargosetu.repo.PermissionRepo;
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
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtil;
    private PermissionRepo permissionRepo;

    public JwtAuthenticationFilter(JwtUtils jwtUtil, PermissionRepo permissionRepo) {
        this.jwtUtil = jwtUtil;
        this.permissionRepo = permissionRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.equals("/pages/login")
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images") ||path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/auth/")
                || path.equals("/");
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws IOException, ServletException {
//
//        String header = request.getHeader("Authorization");
//
//        if (header != null && header.startsWith("Bearer ")) {
//            String token = header.substring(7);
//
//            try {
//                Claims claims = jwtUtil.validateToken(token);
//
//                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//
//                // Roles add ROLE_
//                List<String> roles = claims.get("roles", List.class);
//                Set<String> roleSet = new HashSet<>();
//                if (roles != null) {
//                    roles.forEach(r -> {
//                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
//                        roleSet.add(r); // store in LoggedInUser
//                    });
//                }
//
//                // Permissions → added directly as authorities
//                List<String> permissions = claims.get("permissions", List.class);
//                Set<String> permissionSet = new HashSet<>();
//                if (permissions != null) {
//                    permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
//                    permissionSet.addAll(permissions);
//                }
//
//                LoggedInUser user = new LoggedInUser();
//                user.setUserId(Integer.valueOf(claims.getSubject()));
//                user.setUuid(claims.get("uuid", String.class));
//                user.setEmail(claims.get("email", String.class));
//                user.setUserName(claims.get("userName", String.class));
//                user.setFirstName(claims.get("firstName", String.class));
//                user.setLastName(claims.get("lastName", String.class));
//                user.setRoles(roleSet);
//                user.setPermissions(permissionSet);
//
//                UsernamePasswordAuthenticationToken auth =
//                        new UsernamePasswordAuthenticationToken(user, null, authorities);
//
//                SecurityContextHolder.getContext().setAuthentication(auth);
//
//                System.out.println("LoggedIn user : "+user);
//                System.out.println("Authenticated user: " + user.getUsername() +
//                        ", Authorities: " + authorities);
//
//            } catch (Exception e) {
//                SecurityContextHolder.clearContext();
//                // Optional: log invalid token
//                System.out.println("Invalid JWT: " + e.getMessage());
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws IOException, ServletException {

        // If user already authenticated via OAuth, skip JWT
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.validateToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                List<String> roles = claims.get("roles", List.class);
                Set<String> roleSet = new HashSet<>();
                if (roles != null) {
                    roles.forEach(r -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        roleSet.add(r);
                    });
                }

                List<String> permissions = claims.get("permissions", List.class);
                Set<String> permissionSet = new HashSet<>();
                if (permissions != null) {
                    permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_"+p)));
                    permissionSet.addAll(permissions);
                }

                LoggedInUser user = new LoggedInUser();
                user.setStaffId(Long.valueOf(claims.getSubject()));
                user.setStaffId(claims.get("staffId",Long.class));
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

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                System.out.println("Invalid JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

}

package com.cargosetu.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service("permissionService")
public class PermissionCheckerService {

    /**
     * Checks whether the current authenticated user has access to perform an action on a module.
     *
     * @param module Module code (e.g., CUSTOMER, STAFF, ROLE, DEPARTMENT, TRIP, SHIFTING)
     * @param action Action code (e.g., READ, CREATE, MODIFY, UPDATE, DELETE, VIEW)
     * @return true if user is authorized, false otherwise.
     */
    public boolean hasAccess(String module, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        Set<String> authStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Superadmin or Company Admin roles automatically pass
        if (authStrings.contains("ROLE_SUPERADMIN") || authStrings.contains("ROLE_COMPANY_ADMIN")) {
            return true;
        }

        String normalizedModule = module != null ? module.trim().toUpperCase() : "";
        String normalizedAction = action != null ? action.trim().toUpperCase() : "READ";
        if ("MODIFY".equals(normalizedAction)) {
            normalizedAction = "UPDATE";
        } else if ("VIEW".equals(normalizedAction)) {
            normalizedAction = "READ";
        }

        // PERM_CUSTOMER_READ, PERM_CUSTOMER_CREATE, etc.
        String specificPerm = "PERM_" + normalizedModule + "_" + normalizedAction;
        String allPerm = "PERM_" + normalizedModule + "_ALL";
        String legacyUpdateDeletePerm = "PERM_" + normalizedModule + "_UPDATE_DELETE";

        // Check if any authority matches required permission or full control permission
        for (String authority : authStrings) {
            String upperAuth = authority.toUpperCase();
            if (upperAuth.equals(specificPerm) || 
                upperAuth.equals(allPerm) || 
                (upperAuth.equals(legacyUpdateDeletePerm) && ("UPDATE".equals(normalizedAction) || "DELETE".equals(normalizedAction)))) {
                return true;
            }
        }

        return false;
    }
}

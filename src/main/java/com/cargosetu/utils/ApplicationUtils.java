package com.cargosetu.utils;

import com.cargosetu.config.dto.LoggedInUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ApplicationUtils {

    public static LoggedInUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoggedInUser) {
            return (LoggedInUser) authentication.getPrincipal();
        }
        return null;
    }
}

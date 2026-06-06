package com.cargosetu.utils;

import com.cargosetu.config.dto.LoggedInUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

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

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public Long getLoggedInStaffId() {
        Long staffId = 1L;
        try {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            if (null != securityContext.getAuthentication()) {
                staffId = ((LoggedInUser) securityContext.getAuthentication().getPrincipal()).getStaffId();
            }
        } catch (Exception e) {
            staffId = 1L;
        }
        return staffId;
    }
    public  LoggedInUser getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoggedInUser) {
            return (LoggedInUser) authentication.getPrincipal();
        }
        return null;
    }
}

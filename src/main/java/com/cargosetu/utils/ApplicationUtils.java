package com.cargosetu.utils;

import com.cargosetu.config.dto.LoggedInUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
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

    public static String generateRandomPassword(int length) {
        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "abcdefghijklmnopqrstuvwxyz" +
                        "0123456789" +
                        "!@#$%^&*";

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }

        return password.toString();
    }
    public static Integer getCurrentCompanyId() {
        try {
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.cargosetu.config.dto.LoggedInUser) {
                return ((com.cargosetu.config.dto.LoggedInUser) principal).getCompanyId();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve companyId from SecurityContext", e);
        }
        return null;
    }
}

package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.model.Trip;
import org.springframework.stereotype.Component;

/**
 * Company boundary for trips. Method-level @PreAuthorize is currently disabled
 * (MethodSecurityConfig), so this is the check that keeps one company from reading
 * another company's trips.
 *
 * Ownership is taken from Trip.companyId, falling back to the linked Shifting's
 * companyId (trips created before companyId was populated). Rows with no owner at
 * all stay visible, matching the behaviour before this check existed.
 */
@Component
public class TripAccessPolicy {

    /** Same super-admin convention as ShiftingService (staffId 1 sees every company). */
    static final long SUPER_ADMIN_STAFF_ID = 1L;

    public boolean isSuperAdmin(LoggedInUser user) {
        return user != null && user.getStaffId() != null && user.getStaffId() == SUPER_ADMIN_STAFF_ID;
    }

    public Integer resolveCompanyId(Trip trip) {
        if (trip == null) {
            return null;
        }
        if (trip.getCompanyId() != null) {
            return trip.getCompanyId();
        }
        return trip.getShifting() != null ? trip.getShifting().getCompanyId() : null;
    }

    public boolean canAccess(LoggedInUser user, Trip trip) {
        if (trip == null) {
            return false;
        }
        Integer owner = resolveCompanyId(trip);
        if (owner == null) {
            return true;
        }
        if (user == null) {
            return false;
        }
        return isSuperAdmin(user) || owner.equals(user.getCompanyId());
    }
}

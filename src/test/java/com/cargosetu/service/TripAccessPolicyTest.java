package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.model.Trip;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripAccessPolicyTest {

    private final TripAccessPolicy policy = new TripAccessPolicy();

    private LoggedInUser user(long staffId, Integer companyId) {
        LoggedInUser u = new LoggedInUser();
        u.setStaffId(staffId);
        u.setCompanyId(companyId);
        return u;
    }

    @Test
    void sameCompanyAllowedOtherCompanyDenied() {
        Trip trip = new Trip();
        trip.setCompanyId(5);
        assertThat(policy.canAccess(user(10, 5), trip)).isTrue();
        assertThat(policy.canAccess(user(10, 6), trip)).isFalse();
        assertThat(policy.canAccess(null, trip)).isFalse();
    }

    @Test
    void fallsBackToShiftingCompanyForLegacyTrips() {
        Shifting shifting = new Shifting();
        shifting.setCompanyId(5);
        Trip trip = new Trip();
        trip.setShifting(shifting);
        assertThat(policy.canAccess(user(10, 6), trip)).isFalse();
        assertThat(policy.canAccess(user(10, 5), trip)).isTrue();
    }

    @Test
    void superAdminSeesAllAndUnownedTripsStayVisible() {
        Trip owned = new Trip();
        owned.setCompanyId(5);
        assertThat(policy.canAccess(user(1, null), owned)).isTrue();
        assertThat(policy.canAccess(user(10, 6), new Trip())).isTrue();
    }
}

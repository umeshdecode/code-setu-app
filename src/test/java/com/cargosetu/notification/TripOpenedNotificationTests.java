package com.cargosetu.notification;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.model.Trip;
import com.cargosetu.model.TripStatus;
import com.cargosetu.model.TripStatusHistory;
import com.cargosetu.notification.listener.TripOpenedEventListener;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.repo.TripStatusHistoryRepository;
import com.cargosetu.service.TripService;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the TripOpened admin-notification feature.
 * <p>
 * Deliberately NOT annotated @Transactional: TripOpenedEventListener publishes a
 * GenericNotificationEvent which is handled by the EXISTING GenericNotificationEventListener via
 * @Async + @TransactionalEventListener(AFTER_COMMIT). Spring only fires AFTER_COMMIT listeners
 * once a real transaction actually commits - inside a rolled-back @Transactional test method that
 * never happens (see NotificationFrameworkTests, which sidesteps this by calling
 * NotificationProcessingService#processEvent directly). Here we instead let each repository call
 * commit its own tiny transaction (the default, since there's no enclosing @Transactional) so the
 * real end-to-end async pipeline actually runs, and poll briefly for its (already fast, ~300ms
 * mock-mode) completion. Each test cleans up the rows it created.
 */
@SpringBootTest
class TripOpenedNotificationTests {

    private static final AtomicInteger COMPANY_ID_SEQ = new AtomicInteger(900_000);

    @Autowired
    private TripService tripService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStatusHistoryRepository tripStatusHistoryRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private RolesRepo rolesRepo;

    @Autowired
    private NotificationAuditRepository auditRepository;

    @Autowired
    private TripOpenedEventListener tripOpenedEventListener;

    private final List<Long> createdStaffIds = new ArrayList<>();
    private final List<Long> createdTripIds = new ArrayList<>();
    private final List<Long> createdAuditIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long auditId : createdAuditIds) {
            auditRepository.deleteById(auditId);
        }
        for (Long tripId : createdTripIds) {
            tripStatusHistoryRepository.findByTripTripIdOrderByChangedAtDesc(tripId)
                    .forEach(tripStatusHistoryRepository::delete);
            tripRepository.deleteById(tripId);
        }
        for (Long staffId : createdStaffIds) {
            staffRepository.deleteById(staffId);
        }
        createdAuditIds.clear();
        createdTripIds.clear();
        createdStaffIds.clear();
    }

    private Role resolveCompanyAdminRole() {
        return rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN)
                .orElseGet(() -> rolesRepo.save(new Role(
                        CommonConstants.RolesConstants.COMPANY_ADMIN, "Company Admin",
                        "Company admin role (test-seeded)", "COMPANY", false)));
    }

    private Staff createAdminStaff(Integer companyId, String email) {
        Staff staff = new Staff();
        staff.setUuid(UUID.randomUUID().toString());
        staff.setUserName("trip-admin-" + UUID.randomUUID());
        staff.setFirstName("Trip");
        staff.setLastName("Admin");
        staff.setEmail(email);
        staff.setCompanyId(companyId);
        staff.setIsActive(true);
        staff.setIsDeleted(false);
        HashSet<Role> roles = new HashSet<>();
        roles.add(resolveCompanyAdminRole());
        staff.setRoles(roles);
        staff = staffRepository.save(staff);
        createdStaffIds.add(staff.getStaffId());
        return staff;
    }

    private Trip createTripForCompany(Integer companyId) {
        LoggedInUser user = new LoggedInUser();
        user.setEmail("tester@cargosetu.com");
        user.setUserName("tester");

        TripDTO tripDTO = new TripDTO();
        tripDTO.setTripNumber("TRIP-TEST-" + companyId);
        tripDTO.setStatus(TripStatus.CREATED);
        tripDTO.setPlannedStartTime(LocalDateTime.now().plusHours(2));

        GenericDataDTO response = tripService.createTrip(user, tripDTO);
        assertThat(response.getResponseCode()).isEqualTo(com.cargosetu.utils.CommonStatusCode.CREATED);
        TripDTO savedDto = (TripDTO) response.getData();
        createdTripIds.add(savedDto.getTripId());

        // TripDTO/TripService#createTrip currently has no companyId field/assignment at all -
        // a pre-existing gap in this codebase unrelated to this feature. We set it directly here
        // to simulate the real-world case where a trip belongs to a company, since that's
        // required for the admin-eligibility lookup this feature depends on.
        Trip trip = tripRepository.findById(savedDto.getTripId()).orElseThrow();
        trip.setCompanyId(companyId);
        return tripRepository.save(trip);
    }

    private Optional<NotificationAudit> pollForAudit(String referenceId, String recipient, NotificationStatus status) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            List<NotificationAudit> matches = auditRepository.findAll().stream()
                    .filter(a -> "TRIP_OPENED".equals(a.getTemplateCode()))
                    .filter(a -> referenceId.equals(a.getReferenceId()))
                    .filter(a -> recipient.equals(a.getRecipient()))
                    .filter(a -> status == a.getStatus())
                    .toList();
            if (!matches.isEmpty()) {
                return Optional.of(matches.get(0));
            }
            Thread.sleep(100);
        }
        return Optional.empty();
    }

    @Test
    void testTripCreation_NotifiesAdminWithValidEmail() throws InterruptedException {
        Integer companyId = COMPANY_ID_SEQ.incrementAndGet();
        String adminEmail = "valid-admin-" + companyId + "@cargosetu-test.com";
        createAdminStaff(companyId, adminEmail);
        Trip trip = createTripForCompany(companyId);
        String referenceId = trip.getTripId().toString();

        tripOpenedEventListener.notifyEligibleAdmins(trip.getTripId());

        Optional<NotificationAudit> auditOpt = pollForAudit(referenceId, adminEmail, NotificationStatus.SENT);
        assertThat(auditOpt).as("expected a SENT audit row for the eligible admin").isPresent();

        NotificationAudit audit = auditOpt.get();
        createdAuditIds.add(audit.getId());
        assertThat(audit.getTemplateCode()).isEqualTo("TRIP_OPENED");
        assertThat(audit.getRecipient()).isEqualTo(adminEmail);
        assertThat(audit.getReferenceId()).isEqualTo(referenceId);
        assertThat(audit.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(Boolean.TRUE.equals(audit.getIsHtml())).isTrue();
        assertThat(audit.getSubject()).contains("New Load Available");
    }

    @Test
    void testTripCreation_NoAdminWithValidEmail_NoAuditNoException() throws InterruptedException {
        Integer companyId = COMPANY_ID_SEQ.incrementAndGet();
        // Admin exists but has a blank email - should be filtered out silently.
        createAdminStaff(companyId, "");
        Trip trip = createTripForCompany(companyId);
        String referenceId = trip.getTripId().toString();

        // Should not throw despite there being no valid recipient.
        tripOpenedEventListener.notifyEligibleAdmins(trip.getTripId());

        // Give the (non-existent) async pipeline a brief window, then confirm nothing appeared.
        Thread.sleep(500);
        boolean anyAudit = auditRepository.findAll().stream()
                .anyMatch(a -> "TRIP_OPENED".equals(a.getTemplateCode()) && referenceId.equals(a.getReferenceId()));
        assertThat(anyAudit).isFalse();

        // Trip itself still exists and is unaffected.
        assertThat(tripRepository.findById(trip.getTripId())).isPresent();
    }

    @Test
    void testTripCreation_DuplicateDelivery_IsDeduplicated() throws InterruptedException {
        Integer companyId = COMPANY_ID_SEQ.incrementAndGet();
        String adminEmail = "dedup-admin-" + companyId + "@cargosetu-test.com";
        createAdminStaff(companyId, adminEmail);
        Trip trip = createTripForCompany(companyId);
        String referenceId = trip.getTripId().toString();

        // First call: goes through and sends.
        tripOpenedEventListener.notifyEligibleAdmins(trip.getTripId());
        Optional<NotificationAudit> firstAudit = pollForAudit(referenceId, adminEmail, NotificationStatus.SENT);
        assertThat(firstAudit).as("first delivery should succeed").isPresent();
        createdAuditIds.add(firstAudit.get().getId());

        // Second call for the SAME trip/admin: dedup check must suppress it entirely.
        tripOpenedEventListener.notifyEligibleAdmins(trip.getTripId());
        Thread.sleep(500);

        long sentCount = auditRepository.findAll().stream()
                .filter(a -> "TRIP_OPENED".equals(a.getTemplateCode()))
                .filter(a -> referenceId.equals(a.getReferenceId()))
                .filter(a -> adminEmail.equals(a.getRecipient()))
                .filter(a -> a.getStatus() == NotificationStatus.SENT)
                .count();
        assertThat(sentCount).isEqualTo(1);
    }

    @Test
    void testUpdateTrip_DoesNotProduceAdditionalNotification() throws InterruptedException {
        Integer companyId = COMPANY_ID_SEQ.incrementAndGet();
        String adminEmail = "update-admin-" + companyId + "@cargosetu-test.com";
        createAdminStaff(companyId, adminEmail);
        Trip trip = createTripForCompany(companyId);
        String referenceId = trip.getTripId().toString();

        tripOpenedEventListener.notifyEligibleAdmins(trip.getTripId());
        Optional<NotificationAudit> firstAudit = pollForAudit(referenceId, adminEmail, NotificationStatus.SENT);
        assertThat(firstAudit).isPresent();
        createdAuditIds.add(firstAudit.get().getId());

        long countBefore = auditRepository.findAll().stream()
                .filter(a -> "TRIP_OPENED".equals(a.getTemplateCode()) && referenceId.equals(a.getReferenceId()))
                .count();

        LoggedInUser user = new LoggedInUser();
        user.setEmail("tester@cargosetu.com");
        user.setUserName("tester");
        TripDTO updateDto = new TripDTO();
        updateDto.setStatus(TripStatus.ASSIGNED);
        updateDto.setRemarks("Updated during test");

        GenericDataDTO updateResponse = tripService.updateTrip(user, trip.getTripId(), updateDto);
        assertThat(updateResponse.getResponseCode()).isEqualTo(com.cargosetu.utils.CommonStatusCode.SUCCESS);

        // updateTrip never publishes TripOpenedEvent, so no new audit row should ever appear -
        // brief wait to make sure nothing shows up asynchronously.
        Thread.sleep(500);

        long countAfter = auditRepository.findAll().stream()
                .filter(a -> "TRIP_OPENED".equals(a.getTemplateCode()) && referenceId.equals(a.getReferenceId()))
                .count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}

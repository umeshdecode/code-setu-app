package com.cargosetu.notification.listener;

import com.cargosetu.model.Staff;
import com.cargosetu.model.Trip;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.event.TripOpenedEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the "new load available" admin email whenever a Trip is created (default status
 * CREATED, treated as "available/open" per an explicit product decision - this codebase's
 * TripStatus enum intentionally has no dedicated OPEN/AVAILABLE value).
 * <p>
 * This listener contains ONLY eligibility/dedup orchestration. The actual template lookup,
 * rendering, audit-row creation and SMTP dispatch is handled entirely by the existing
 * GenericNotificationEventListener / NotificationProcessingService pipeline - this class simply
 * publishes one GenericNotificationEvent per eligible, not-yet-notified admin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripOpenedEventListener {

    private static final String TEMPLATE_CODE = "TRIP_OPENED";
    private static final String EMAIL_FORMAT_REGEX = ".+@.+\\..+";
    private static final String FALLBACK_ROUTE_LABEL = "Route TBD";

    private final TripRepository tripRepository;
    private final StaffRepository staffRepository;
    private final NotificationAuditRepository auditRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Listens for TripOpenedEvent. Executed asynchronously on the notification thread pool AFTER
     * the trip-creating transaction commits, mirroring GenericNotificationEventListener exactly.
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTripOpenedEvent(TripOpenedEvent event) {
        log.info("[TripOpened Listener Thread: {}] Intercepted TripOpenedEvent for tripId: {}",
                Thread.currentThread().getName(), event.tripId());
        try {
            notifyEligibleAdmins(event.tripId());
        } catch (Exception e) {
            log.error("Failed to process TripOpenedEvent for tripId: {}", event.tripId(), e);
        }
    }

    /**
     * Core orchestration logic, extracted from the @TransactionalEventListener entry point so
     * tests can invoke it directly and synchronously (bypassing @Async/AFTER_COMMIT), exactly as
     * NotificationFrameworkTests calls NotificationProcessingService#processEvent directly.
     */
    public void notifyEligibleAdmins(Long tripId) {
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                log.warn("TripOpenedEvent: trip {} could not be found (possibly deleted since commit); skipping admin notification.", tripId);
                return;
            }
            Trip trip = tripOptional.get();

            List<Staff> admins = staffRepository.findActiveAdminsByCompanyIdAndRoleCodes(
                    trip.getCompanyId(),
                    List.of(CommonConstants.RolesConstants.SUPERADMIN, CommonConstants.RolesConstants.COMPANY_ADMIN)
            );

            String referenceId = tripId.toString();
            Map<String, Object> variables = buildVariables(trip);

            for (Staff admin : admins) {
                String email = admin.getEmail();
                if (email == null || email.isBlank()) {
                    continue;
                }
                String cleanEmail = email.trim();
                if (!cleanEmail.matches(EMAIL_FORMAT_REGEX)) {
                    continue;
                }

                boolean alreadyNotified = auditRepository.existsByTemplateCodeAndReferenceIdAndRecipientAndStatus(
                        TEMPLATE_CODE, referenceId, cleanEmail, NotificationStatus.SENT);
                if (alreadyNotified) {
                    log.debug("Skipping duplicate TRIP_OPENED notification to {} for trip {}", cleanEmail, tripId);
                    continue;
                }

                eventPublisher.publishEvent(new GenericNotificationEvent(
                        TEMPLATE_CODE,
                        List.of(cleanEmail),
                        NotificationChannel.EMAIL,
                        variables,
                        referenceId
                ));
            }
        } catch (Exception e) {
            log.error("Error while notifying admins for TripOpenedEvent tripId: {}", tripId, e);
        }
    }

    private Map<String, Object> buildVariables(Trip trip) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("tripId", trip.getTripId());
        variables.put("tripNumber", trip.getTripNumber() != null ? trip.getTripNumber() : String.valueOf(trip.getTripId()));

        String sourceCity = null;
        String destinationCity = null;
        if (trip.getShifting() != null) {
            sourceCity = trip.getShifting().getShiftFromCity();
            destinationCity = trip.getShifting().getShiftToCity();
        }
        variables.put("sourceCity", (sourceCity != null && !sourceCity.isBlank()) ? sourceCity : FALLBACK_ROUTE_LABEL);
        variables.put("destinationCity", (destinationCity != null && !destinationCity.isBlank()) ? destinationCity : FALLBACK_ROUTE_LABEL);

        if (trip.getPlannedStartTime() != null) {
            variables.put("plannedStartTime", trip.getPlannedStartTime().toString());
        }
        if (trip.getPlannedEenTime() != null) {
            variables.put("plannedEenTime", trip.getPlannedEenTime().toString());
        }

        if (trip.getVehicle() != null && trip.getVehicle().getRegistrationNumber() != null) {
            variables.put("vehicleNumber", trip.getVehicle().getRegistrationNumber());
        }
        if (trip.getVehicleTypeCategory() != null && !trip.getVehicleTypeCategory().isBlank()) {
            variables.put("vehicleTypeCategory", trip.getVehicleTypeCategory());
        }

        if (trip.getRemainingWeightKg() != null) {
            variables.put("remainingWeightKg", trip.getRemainingWeightKg());
        }
        if (trip.getRemainingVolumeCbm() != null) {
            variables.put("remainingVolumeCbm", trip.getRemainingVolumeCbm());
        }

        // Optional HTML sections: the template engine is a plain {{placeholder}} regex
        // substitution with no conditional-block support, so we pre-decide in Java which
        // sections apply and pass each as a single pre-rendered HTML fragment.
        variables.put("capacitySection", buildCapacitySection(trip));
        // No direct Trip -> Shipment/TripShipment linkage exists on this entity, so the cargo
        // section is intentionally omitted entirely for this initial version.
        variables.put("cargoSection", "");

        variables.put("viewTripUrl", frontendUrl + "/trips");

        return variables;
    }

    private String buildCapacitySection(Trip trip) {
        if (trip.getRemainingWeightKg() == null && trip.getRemainingVolumeCbm() == null) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        if (trip.getRemainingWeightKg() != null) {
            rows.append("<tr>")
                    .append("<td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Available Weight</td>")
                    .append("<td style=\"padding:8px 12px;\">").append(trip.getRemainingWeightKg()).append(" kg</td>")
                    .append("</tr>");
        }
        if (trip.getRemainingVolumeCbm() != null) {
            rows.append("<tr>")
                    .append("<td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Available Volume</td>")
                    .append("<td style=\"padding:8px 12px;\">").append(trip.getRemainingVolumeCbm()).append(" cbm</td>")
                    .append("</tr>");
        }
        return "<tr><td colspan=\"2\" style=\"padding:12px 0 4px 0;font-weight:bold;color:#4663AC;\">Available Capacity</td></tr>"
                + "<tr><td colspan=\"2\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#E1F1FD;border:1px solid #D2DEEB;border-radius:6px;\">"
                + rows
                + "</table>"
                + "</td></tr>";
    }
}

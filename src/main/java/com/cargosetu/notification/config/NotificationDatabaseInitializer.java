package com.cargosetu.notification.config;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDatabaseInitializer implements CommandLineRunner {

    private final NotificationTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing default notification templates in the database...");
        List<NotificationTemplate> defaultTemplates = new ArrayList<>();

        // 1. CUSTOMER_CREATED - Email
        defaultTemplates.add(createTemplate(
                "CUSTOMER_CREATED",
                NotificationChannel.EMAIL,
                "Welcome to CargoSetu, {{name}}!",
                "Dear {{name}},\n\nYour account has been successfully created with customer ID: {{customerId}}.\n\nMobile Number: {{mobileNo}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 2. CUSTOMER_CREATED - SMS
        defaultTemplates.add(createTemplate(
                "CUSTOMER_CREATED",
                NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to CargoSetu! Your account is active."
        ));

        // 3. CUSTOMER_UPDATED - Email
        defaultTemplates.add(createTemplate(
                "CUSTOMER_UPDATED",
                NotificationChannel.EMAIL,
                "CargoSetu Account Updated",
                "Dear {{name}},\n\nYour customer account details have been successfully updated.\n\nBest regards,\nCargoSetu Team"
        ));

        // 4. INQUIRY_CREATED - Email (Customer acknowledgment)
        defaultTemplates.add(createTemplate(
                "INQUIRY_CREATED",
                NotificationChannel.EMAIL,
                "Inquiry Received - {{inquiryNumber}}",
                "Dear {{name}},\n\nThank you for contacting CargoSetu. We have received your inquiry regarding \"{{subject}}\".\n\nPlease be assured that your request are in progress. Our support team will get back to you shortly.\n\nInquiry Number: {{inquiryNumber}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 5. INQUIRY_CREATED_SUPPORT - Email (Support Alert)
        defaultTemplates.add(createTemplate(
                "INQUIRY_CREATED_SUPPORT",
                NotificationChannel.EMAIL,
                "New Customer Inquiry - {{inquiryNumber}}",
                "Hello Support Team,\n\nA new customer inquiry has been submitted.\n\nDetails:\n- Inquiry Number: {{inquiryNumber}}\n- Name: {{name}}\n- Email: {{email}}\n- Phone: {{phone}}\n- Subject: {{subject}}\n- Message: {{message}}\n\nBest regards,\nCargoSetu System"
        ));

        // 6. INQUIRY_REPLY - Email
        defaultTemplates.add(createTemplate(
                "INQUIRY_REPLY",
                NotificationChannel.EMAIL,
                "Response to Inquiry {{inquiryNumber}}",
                "Dear {{name}},\n\nWe have responded to your inquiry regarding \"{{subject}}\".\n\nReply:\n{{replyMessage}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 7. STAFF_CREATED - Email
        defaultTemplates.add(createTemplate(
                "STAFF_CREATED",
                NotificationChannel.EMAIL,
                "Welcome to the Team, {{name}}!",
                "Dear {{name}},\n\nYour staff account has been successfully created with username: {{username}} and staff ID: {{staffId}}.\n\nBest regards,\nCargoSetu Team"
        ));

        // 8. STAFF_CREATED - SMS
        defaultTemplates.add(createTemplate(
                "STAFF_CREATED",
                NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to the team! Your username is {{username}}."
        ));

        // 9. VEHICLE_ASSIGNED - Email
        defaultTemplates.add(createTemplate(
                "VEHICLE_ASSIGNED",
                NotificationChannel.EMAIL,
                "Vehicle Assigned - {{bookingNumber}}",
                "Dear {{name}},\n\nVehicle {{vehicleNumber}} has been assigned to your booking {{bookingNumber}}.\n\nDriver: {{driverName}}\nDriver Contact: {{driverPhone}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 10. BOOKING_CREATED - Email
        defaultTemplates.add(createTemplate(
                "BOOKING_CREATED",
                NotificationChannel.EMAIL,
                "Booking Confirmed - {{bookingNumber}}",
                "Dear {{name}},\n\nYour booking {{bookingNumber}} has been confirmed.\n\nPickup: {{pickupLocation}}\nDelivery: {{deliveryLocation}}\nAmount: {{amount}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 11. BOOKING_CREATED - SMS
        defaultTemplates.add(createTemplate(
                "BOOKING_CREATED",
                NotificationChannel.SMS,
                null,
                "Booking confirmed: {{bookingNumber}}. Pickup: {{pickupLocation}}. Thanks for choosing CargoSetu!"
        ));

        // 12. INVOICE_GENERATED - Email
        defaultTemplates.add(createTemplate(
                "INVOICE_GENERATED",
                NotificationChannel.EMAIL,
                "Invoice Generated - {{invoiceNumber}}",
                "Dear {{name}},\n\nAn invoice {{invoiceNumber}} has been generated for your booking {{bookingNumber}}.\n\nTotal Due: {{totalAmount}}\nDue Date: {{dueDate}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 13. PAYMENT_RECEIVED - Email
        defaultTemplates.add(createTemplate(
                "PAYMENT_RECEIVED",
                NotificationChannel.EMAIL,
                "Payment Confirmation - {{paymentId}}",
                "Dear {{name}},\n\nWe have received your payment of {{amount}} for invoice {{invoiceNumber}}.\n\nTransaction ID: {{transactionId}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 14. OTP_SMS - SMS
        defaultTemplates.add(createTemplate(
                "OTP_SMS",
                NotificationChannel.SMS,
                null,
                "Your CargoSetu OTP is {{otpCode}}. Please do not share it with anyone."
        ));

        // 15. TRIP_OPENED - Email (HTML, sent to company Admin staff when a new Trip is created)
        defaultTemplates.add(createHtmlTemplate(
                "TRIP_OPENED",
                NotificationChannel.EMAIL,
                "New Load Available: {{sourceCity}} → {{destinationCity}}",
                TRIP_OPENED_EMAIL_BODY
        ));

        // Save templates if not already present
        for (NotificationTemplate t : defaultTemplates) {
            templateRepository.findByTemplateCodeAndChannel(t.getTemplateCode(), t.getChannel())
                    .ifPresentOrElse(
                            existing -> log.debug("Template already exists for code: {} and channel: {}", t.getTemplateCode(), t.getChannel()),
                            () -> {
                                templateRepository.save(t);
                                log.info("Saved default template for code: {} and channel: {}", t.getTemplateCode(), t.getChannel());
                            }
                    );
        }
        log.info("Notification template initialization complete.");
    }

    private NotificationTemplate createTemplate(String code, NotificationChannel channel, String subject, String body) {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(code);
        template.setChannel(channel);
        template.setSubjectTemplate(subject);
        template.setBodyTemplate(body);
        template.setIsActive(true);
        return template;
    }

    private NotificationTemplate createHtmlTemplate(String code, NotificationChannel channel, String subject, String body) {
        NotificationTemplate template = createTemplate(code, channel, subject, body);
        template.setIsHtml(true);
        return template;
    }

    // Table-based layout with inline CSS only (no <style> blocks / flexbox) so this renders
    // correctly across email clients. Brand colors per the frontend theme
    // (code-setu-app-ui/src/styles.css): #4663AC primary, #E1F1FD background tint,
    // #C8D9ED / #C1D8F0 supporting surfaces, #D2DEEB borders.
    private static final String TRIP_OPENED_EMAIL_BODY =
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#E1F1FD;padding:24px 0;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border:1px solid #D2DEEB;border-radius:8px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;\">"
            + "<tr><td style=\"background-color:#4663AC;padding:20px 24px;\">"
            + "<span style=\"color:#ffffff;font-size:20px;font-weight:bold;\">CargoSetu &mdash; New Load Available</span>"
            + "</td></tr>"
            + "<tr><td style=\"padding:24px;\">"
            + "<p style=\"margin:0 0 16px 0;color:#333333;font-size:15px;\">"
            + "A new trip has just been opened and is available for booking. Details are below:"
            + "</p>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#C8D9ED;border:1px solid #D2DEEB;border-radius:6px;margin-bottom:16px;\">"
            + "<tr><td colspan=\"2\" style=\"padding:12px 12px 4px 12px;font-weight:bold;color:#4663AC;\">Trip Details</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;width:40%;\">Trip ID</td><td style=\"padding:8px 12px;\">{{tripNumber}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">From</td><td style=\"padding:8px 12px;\">{{sourceCity}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">To</td><td style=\"padding:8px 12px;\">{{destinationCity}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Departure</td><td style=\"padding:8px 12px;\">{{plannedStartTime}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Expected Delivery</td><td style=\"padding:8px 12px;\">{{plannedEenTime}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Vehicle Type</td><td style=\"padding:8px 12px;\">{{vehicleTypeCategory}}</td></tr>"
            + "<tr><td style=\"padding:8px 12px;color:#4663AC;font-weight:bold;\">Vehicle Number</td><td style=\"padding:8px 12px;\">{{vehicleNumber}}</td></tr>"
            + "</table>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:16px;\">"
            + "{{capacitySection}}"
            + "{{cargoSection}}"
            + "</table>"
            + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\">"
            + "<tr><td style=\"background-color:#4663AC;border-radius:6px;\">"
            + "<a href=\"{{viewTripUrl}}\" style=\"display:inline-block;padding:12px 28px;color:#ffffff;font-weight:bold;font-size:15px;text-decoration:none;\">View Trip</a>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr>"
            + "<tr><td style=\"background-color:#C1D8F0;padding:16px 24px;\">"
            + "<p style=\"margin:0;color:#4663AC;font-size:12px;\">"
            + "This is an automated notification from CargoSetu. Please do not reply to this email."
            + "</p>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr>"
            + "</table>";
}

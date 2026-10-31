package com.cargosetu.notification.config;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;
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
                NotificationEventType.CUSTOMER_CREATED,
                NotificationChannel.EMAIL,
                "Welcome to CargoSetu, {{name}}!",
                "Dear {{name}},\n\nYour account has been successfully created with customer ID: {{customerId}}.\n\nMobile Number: {{mobileNo}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 2. CUSTOMER_CREATED - SMS
        defaultTemplates.add(createTemplate(
                NotificationEventType.CUSTOMER_CREATED,
                NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to CargoSetu! Your account is active."
        ));

        // 3. CUSTOMER_UPDATED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.CUSTOMER_UPDATED,
                NotificationChannel.EMAIL,
                "CargoSetu Account Updated",
                "Dear {{name}},\n\nYour customer account details have been successfully updated.\n\nBest regards,\nCargoSetu Team"
        ));

        // 4. INQUIRY_CREATED - Email (Customer acknowledgment)
        defaultTemplates.add(createTemplate(
                NotificationEventType.INQUIRY_CREATED,
                NotificationChannel.EMAIL,
                "Inquiry Received - {{inquiryNumber}}",
                "Dear {{name}},\n\nThank you for contacting CargoSetu. We have received your inquiry regarding \"{{subject}}\".\n\nPlease be assured that your request are in progress. Our support team will get back to you shortly.\n\nInquiry Number: {{inquiryNumber}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 5. INQUIRY_CREATED_SUPPORT - Email (Support Alert)
        defaultTemplates.add(createTemplate(
                NotificationEventType.INQUIRY_CREATED_SUPPORT,
                NotificationChannel.EMAIL,
                "New Customer Inquiry - {{inquiryNumber}}",
                "Hello Support Team,\n\nA new customer inquiry has been submitted.\n\nDetails:\n- Inquiry Number: {{inquiryNumber}}\n- Name: {{name}}\n- Email: {{email}}\n- Phone: {{phone}}\n- Subject: {{subject}}\n- Message: {{message}}\n\nBest regards,\nCargoSetu System"
        ));

        // 6. INQUIRY_REPLY - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.INQUIRY_REPLY,
                NotificationChannel.EMAIL,
                "Response to Inquiry {{inquiryNumber}}",
                "Dear {{name}},\n\nWe have responded to your inquiry regarding \"{{subject}}\".\n\nReply:\n{{replyMessage}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 7. STAFF_CREATED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.STAFF_CREATED,
                NotificationChannel.EMAIL,
                "Welcome to the Team, {{name}}!",
                "Dear {{name}},\n\nYour staff account has been successfully created with username: {{username}} and staff ID: {{staffId}}.\n\nBest regards,\nCargoSetu Team"
        ));

        // 8. STAFF_CREATED - SMS
        defaultTemplates.add(createTemplate(
                NotificationEventType.STAFF_CREATED,
                NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to the team! Your username is {{username}}."
        ));

        // 9. VEHICLE_ASSIGNED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.VEHICLE_ASSIGNED,
                NotificationChannel.EMAIL,
                "Vehicle Assigned - {{bookingNumber}}",
                "Dear {{name}},\n\nVehicle {{vehicleNumber}} has been assigned to your booking {{bookingNumber}}.\n\nDriver: {{driverName}}\nDriver Contact: {{driverPhone}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 10. BOOKING_CREATED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.BOOKING_CREATED,
                NotificationChannel.EMAIL,
                "Booking Confirmed - {{bookingNumber}}",
                "Dear {{name}},\n\nYour booking {{bookingNumber}} has been confirmed.\n\nPickup: {{pickupLocation}}\nDelivery: {{deliveryLocation}}\nAmount: {{amount}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 11. BOOKING_CREATED - SMS
        defaultTemplates.add(createTemplate(
                NotificationEventType.BOOKING_CREATED,
                NotificationChannel.SMS,
                null,
                "Booking confirmed: {{bookingNumber}}. Pickup: {{pickupLocation}}. Thanks for choosing CargoSetu!"
        ));

        // 12. INVOICE_GENERATED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.INVOICE_GENERATED,
                NotificationChannel.EMAIL,
                "Invoice Generated - {{invoiceNumber}}",
                "Dear {{name}},\n\nAn invoice {{invoiceNumber}} has been generated for your booking {{bookingNumber}}.\n\nTotal Due: {{totalAmount}}\nDue Date: {{dueDate}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 13. PAYMENT_RECEIVED - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.PAYMENT_RECEIVED,
                NotificationChannel.EMAIL,
                "Payment Confirmation - {{paymentId}}",
                "Dear {{name}},\n\nWe have received your payment of {{amount}} for invoice {{invoiceNumber}}.\n\nTransaction ID: {{transactionId}}\n\nBest regards,\nCargoSetu Team"
        ));

        // 14. OTP_SMS - SMS
        defaultTemplates.add(createTemplate(
                NotificationEventType.OTP_SMS,
                NotificationChannel.SMS,
                null,
                "Your CargoSetu OTP is {{otpCode}}. Please do not share it with anyone."
        ));

        // 15. TRIP_ASSIGNED_STAFF - PUSH (subject doubles as the push notification title)
        defaultTemplates.add(createTemplate(
                NotificationEventType.TRIP_ASSIGNED_STAFF,
                NotificationChannel.PUSH,
                "Trip {{tripNumber}} assigned",
                "{{driverName}} is assigned to trip {{tripNumber}}: {{sourceCity}} -> {{destinationCity}}"
        ));

        // 16. CHAT_MESSAGE_RECEIVED - PUSH (subject doubles as the push notification title)
        defaultTemplates.add(createTemplate(
                NotificationEventType.CHAT_MESSAGE_RECEIVED,
                NotificationChannel.PUSH,
                "New message from {{senderName}}",
                "{{messagePreview}}"
        ));

        // 17. LOGIN - PUSH (subject doubles as the push notification title)
        defaultTemplates.add(createTemplate(
                NotificationEventType.LOGIN,
                NotificationChannel.PUSH,
                "New login to your account",
                "Hello {{name}}, a new login to your CargoSetu account was detected on {{loginTime}} from {{device}}."
        ));

        // 18. REGISTRATION - Email (company/tenant onboarding welcome)
        defaultTemplates.add(createTemplate(
                NotificationEventType.REGISTRATION,
                NotificationChannel.EMAIL,
                "Welcome to CargoSetu, {{companyName}}!",
                "<p>Dear {{contactName}},</p><p>Your CargoSetu account for <strong>{{companyName}}</strong> has been successfully registered.</p><p th:if=\"${loginUrl}\">You can sign in here: <a th:href=\"${loginUrl}\" th:text=\"${loginUrl}\">link</a></p><p>Best regards,<br/>CargoSetu Team</p>"
        ));

        // 19. TRIP_START - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.TRIP_START,
                NotificationChannel.EMAIL,
                "Trip {{tripNumber}} has started",
                "<p>Dear {{name}},</p><p>Your trip <strong>{{tripNumber}}</strong> from {{sourceCity}} to {{destinationCity}} has started.</p><p>Driver: {{driverName}}<br/>Driver Contact: {{driverPhone}}</p><p>Best regards,<br/>CargoSetu Team</p>"
        ));

        // 20. TRIP_START - PUSH
        defaultTemplates.add(createTemplate(
                NotificationEventType.TRIP_START,
                NotificationChannel.PUSH,
                "Trip {{tripNumber}} started",
                "{{driverName}} has started trip {{tripNumber}}: {{sourceCity}} -> {{destinationCity}}"
        ));

        // 21. TRIP_END - Email
        defaultTemplates.add(createTemplate(
                NotificationEventType.TRIP_END,
                NotificationChannel.EMAIL,
                "Trip {{tripNumber}} completed",
                "<p>Dear {{name}},</p><p>Your trip <strong>{{tripNumber}}</strong> from {{sourceCity}} to {{destinationCity}} has been completed.</p><p>Thank you for choosing CargoSetu.</p>"
        ));

        // 22. TRIP_END - PUSH
        defaultTemplates.add(createTemplate(
                NotificationEventType.TRIP_END,
                NotificationChannel.PUSH,
                "Trip {{tripNumber}} completed",
                "Trip {{tripNumber}} ({{sourceCity}} -> {{destinationCity}}) has been completed."
        ));

        // 23. DRIVER_AVAILABLE - PUSH (notify matching load-posters)
        defaultTemplates.add(createTemplate(
                NotificationEventType.DRIVER_AVAILABLE,
                NotificationChannel.PUSH,
                "Driver available near {{location}}",
                "A driver with vehicle {{vehicleNumber}} is now available near {{location}}."
        ));

        // 24. LOAD_AVAILABLE - PUSH (notify matching drivers/trucks)
        defaultTemplates.add(createTemplate(
                NotificationEventType.LOAD_AVAILABLE,
                NotificationChannel.PUSH,
                "New load available: {{sourceCity}} -> {{destinationCity}}",
                "A new load is available from {{sourceCity}} to {{destinationCity}}. Tap to view details."
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

    private NotificationTemplate createTemplate(NotificationEventType code, NotificationChannel channel, String subject, String body) {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(code);
        template.setChannel(channel);
        template.setSubjectTemplate(subject);
        template.setBodyTemplate(body);
        template.setIsActive(true);
        return template;
    }
}

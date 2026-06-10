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
}

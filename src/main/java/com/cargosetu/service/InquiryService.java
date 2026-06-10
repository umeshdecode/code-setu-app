package com.cargosetu.service;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.model.CustomerInquiry;
import com.cargosetu.model.InquiryStatus;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.repo.InquiryRepository;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.notification.email.support}")
    private String supportEmail;

    public ApiResponseDto createInquiry(InquiryRequest request) {

        log.info("Received inquiry request from email={}, subject={}",
                request.getEmail(),
                request.getSubject());

        try {

            String inquiryNo = generateInquiryNumber();

            CustomerInquiry inquiry = new CustomerInquiry();

            inquiry.setInquiryNumber(inquiryNo);
            inquiry.setName(request.getName());
            inquiry.setEmail(request.getEmail());
            inquiry.setPhone(request.getPhone());
            inquiry.setSubject(request.getSubject());
            inquiry.setMessage(request.getMessage());
            inquiry.setStatus(InquiryStatus.NEW);
            inquiry.setSource("WEBSITE");
            inquiry.setCreatedAt(LocalDateTime.now());

            repository.save(inquiry);

            log.info("Inquiry saved successfully. inquiryNo={}, id={}",
                    inquiryNo,
                    inquiry.getId());

            /*
             * Publish event after successful DB save.
             */
            // Publish generic events for customer acknowledgment and support team alert
            Map<String, Object> variables = Map.of(
                "inquiryNumber", inquiryNo,
                "name", request.getName() != null ? request.getName() : "",
                "email", request.getEmail() != null ? request.getEmail() : "",
                "phone", request.getPhone() != null ? request.getPhone() : "",
                "subject", request.getSubject() != null ? request.getSubject() : "",
                "message", request.getMessage() != null ? request.getMessage() : ""
            );

            // 1. Customer Acknowledgment
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "INQUIRY_CREATED",
                    List.of(request.getEmail()),
                    NotificationChannel.EMAIL,
                    variables
                ));
            }

            // 2. Support Alert
            if (supportEmail != null && !supportEmail.isBlank()) {
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "INQUIRY_CREATED_SUPPORT",
                    List.of(supportEmail),
                    NotificationChannel.EMAIL,
                    variables
                ));
            }

            log.info("InquiryCreatedEvent published successfully. inquiryNo={}",
                    inquiryNo);

            return ApiResponseDto.builder()
                    .responseCode(HttpStatus.SC_OK)
                    .responseMessage("Inquiry submitted successfully.")
                    .responseBody(inquiryNo)
                    .build();

        } catch (DataAccessException ex) {

            log.error("Database error while creating inquiry for email={}",
                    request.getEmail(),
                    ex);

            throw new RuntimeException(
                    "Unable to save inquiry. Please try again later.");
        } catch (Exception ex) {

            log.error("Unexpected error while creating inquiry for email={}",
                    request.getEmail(),
                    ex);

            throw new RuntimeException(
                    "Something went wrong while processing your request.") {
            };
        }
    }

    private String generateInquiryNumber() {

        return "INC-" +
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

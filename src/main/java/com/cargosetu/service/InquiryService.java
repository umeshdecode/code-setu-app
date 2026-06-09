package com.cargosetu.service;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.model.CustomerInquiry;
import com.cargosetu.model.InquiryStatus;
import com.cargosetu.notification.event.InquiryCreatedEvent;
import com.cargosetu.repo.InquiryRepository;
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
             * Email failures should not impact inquiry creation.
             */
            eventPublisher.publishEvent(
                    new InquiryCreatedEvent(
                            inquiryNo,
                            request.getName(),
                            request.getEmail(),
                            request.getPhone(),
                            request.getSubject(),
                            request.getMessage()
                    )
            );

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

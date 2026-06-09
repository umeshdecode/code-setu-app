package com.cargosetu.service;

import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.model.CustomerInquiry;
import com.cargosetu.model.InquiryStatus;
import com.cargosetu.notification.event.InquiryCreatedEvent;
import com.cargosetu.repo.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public String createInquiry(InquiryRequest request) {

        String inquiryNo = "INC-" + System.currentTimeMillis();

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

        // Publish event for decoupled asynchronous notification dispatching post-commit
        eventPublisher.publishEvent(new InquiryCreatedEvent(
            inquiryNo,
            request.getName(),
            request.getEmail(),
            request.getPhone(),
            request.getSubject(),
            request.getMessage()
        ));

        return inquiryNo;
    }
}

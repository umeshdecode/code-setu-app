package com.cargosetu.controller;

import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService
            inquiryService;

    @PostMapping
    public ResponseEntity<String> submit(
            @RequestBody InquiryRequest request) {

        String inquiryNo = inquiryService.createInquiry(request);

        return ResponseEntity.ok(
                "Inquiry submitted successfully. Ref No: "
                        + inquiryNo);
    }
}

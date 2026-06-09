package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.service.InquiryService;
import com.cargosetu.utils.BaseURL;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BaseURL.PUBLIC_PLAN+ BaseURL.INQUERY)
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService
            inquiryService;

    @PostMapping
    public ApiResponseDto submit(
            @RequestBody InquiryRequest request) {

        return inquiryService.createInquiry(request);
    }
}

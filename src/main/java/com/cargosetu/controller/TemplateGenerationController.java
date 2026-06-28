package com.cargosetu.controller;

import com.cargosetu.model.Template;
import com.cargosetu.service.TemplateService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL + BaseURL.TEMPLATES)
@Slf4j
public class TemplateGenerationController {

    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    public TemplateGenerationController(TemplateService templateService, ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addTemplate(@RequestPart("file") MultipartFile file, @RequestPart("data") String payload) throws JsonProcessingException {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.FORBIDDEN);
        response.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
        try {
            List<Template> template = objectMapper.readValue(payload, new TypeReference<List<Template>>() {
            });
            templateService.createTemplate(file, template.get(0));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege(CommonStatusCode.MSG_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }


        @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @GetMapping(value = "/quotation/{shiftingId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> quotationPdf(@PathVariable Long shiftingId) throws Exception {
        byte[] pdf = templateService.generateQuotationPdf(shiftingId);
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"quotation-" + shiftingId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            GenericDataDTO errorResponse = new GenericDataDTO();
            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            errorResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            errorResponse.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
//    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
//    @GetMapping(value = "/quotation/{shiftingId}/pdf")
//    public ResponseEntity<?> quotationPdf(@PathVariable Long shiftingId) throws Exception {
//        String pdf = templateService.generateQuotationPdf(shiftingId);
//        try {
//            return ResponseEntity.ok().body(pdf);
//        } catch (Exception e) {
//            GenericDataDTO errorResponse = new GenericDataDTO();
//            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
//            errorResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
//            errorResponse.setErrors(new Object[]{e.getMessage()});
//            return ResponseEntity.internalServerError().body(errorResponse);
//        }
//    }

    @GetMapping(value = "/{shiftingId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> getInvoiceHtml(@PathVariable Long shiftingId) throws Exception {
        byte[] pdf = templateService.generateInvoiceHtml(shiftingId);
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"quotation-" + shiftingId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            GenericDataDTO errorResponse = new GenericDataDTO();
            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            errorResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            errorResponse.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

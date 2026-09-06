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
import org.springframework.data.domain.Page;
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

    @GetMapping("/by-type")
    public ResponseEntity<?> getTemplatesByType(
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Page<com.cargosetu.model.Template> templates = templateService.getTemplatesByType(type, pageNumber, pageSize);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Templates fetched successfully");
            response.setData(templates);
        } catch (Exception e) {
            log.error("Error fetching templates by type: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultTemplate(@PathVariable Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            templateService.setDefaultTemplate(id);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Default template set successfully");
        } catch (Exception e) {
            log.error("Error setting default template: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}/preview-html", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    public ResponseEntity<?> getTemplateHtml(@PathVariable Long id) {
        try {
            String html = templateService.getTemplateHtml(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("Error fetching template HTML for preview: {}", e.getMessage(), e);
            GenericDataDTO errorResponse = new GenericDataDTO();
            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            errorResponse.setResponseMessege(e.getMessage() != null ? e.getMessage() : CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            errorResponse.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @PreAuthorize("hasAnyAuthority(" +
            "T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE, " +
            "T(com.cargosetu.utils.PermissionConstants).COMPANY_READ, " +
            "T(com.cargosetu.utils.PermissionConstants).CUSTOMER_VIEW, " +
            "T(com.cargosetu.utils.PermissionConstants).SHIFT_READ)")
    @GetMapping(value = "/quotation/{shiftingId}/pdf", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    public ResponseEntity<?> quotationPdf(@PathVariable Long shiftingId,
                                          @RequestParam(required = false) Long templateId) {
        try {
            byte[] pdf = templateService.generateQuotationPdf(shiftingId, templateId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"quotation-" + shiftingId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating quotation PDF: {}", e.getMessage(), e);
            GenericDataDTO errorResponse = new GenericDataDTO();
            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            errorResponse.setResponseMessege(e.getMessage() != null ? e.getMessage() : CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            errorResponse.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @PostMapping(value = "/quotation/{shiftingId}/send")
    public ResponseEntity<?> sendQuotation(@PathVariable Long shiftingId,
                                           @RequestParam(required = false) Long templateId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            templateService.generateAndSendQuotationPdf(shiftingId, templateId);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Quotation generated and sent to customer successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating or sending quotation: ", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege(e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping(value = "/{shiftingId}/invoice", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    public ResponseEntity<?> getInvoiceHtml(@PathVariable Long shiftingId) {
        try {
            byte[] pdf = templateService.generateInvoiceHtml(shiftingId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"quotation-" + shiftingId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating invoice PDF: {}", e.getMessage(), e);
            GenericDataDTO errorResponse = new GenericDataDTO();
            errorResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            errorResponse.setResponseMessege(e.getMessage() != null ? e.getMessage() : CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            errorResponse.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }
}

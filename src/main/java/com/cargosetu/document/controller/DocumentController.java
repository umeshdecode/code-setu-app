package com.cargosetu.document.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.document.service.DocumentMasterService;
import com.cargosetu.document.service.DocumentUploadService;
import com.cargosetu.dto.DocumentMasterDTO;
import com.cargosetu.dto.DocumentUploadDTO;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping(value = BaseURL.BASE_URL + BaseURL.DOCS_UPLOADS)
public class DocumentController {

    @Autowired
    private DocumentMasterService documentMasterService;
    @Autowired
    private DocumentUploadService documentUploadService;

    @PostMapping(value = UrlConstants.DOCUMENT_MASTER)
    public GenericDataDTO saveMasterDocument(@RequestBody DocumentMasterDTO documentMasterDTO) {
        GenericDataDTO dto = new GenericDataDTO();
        boolean isSaved = documentMasterService.saveDocumentMaster(documentMasterDTO);
        if(isSaved){
            dto.setResponseCode(CommonStatusCode.SUCCESS);
            dto.setResponseMessege("Document Master Saved Successfully!");
        }else {
            dto.setResponseCode(CommonStatusCode.FAIL);
            dto.setResponseMessege("Document Master Not Saved!");
        }
        return dto;
    }

    @PostMapping(value = UrlConstants.UPLOAD_DOCUMENT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestPart("file") MultipartFile file, @RequestPart("data") String requestDto) throws JsonProcessingException {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.FAIL);
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        DocumentUploadDTO dto = mapper.readValue(requestDto, DocumentUploadDTO.class);
        response.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
        ValidationResponse validReqDto = documentUploadService.validateDocumentUpload(dto);
        if (validReqDto.isValid()) {
            ValidationResponse validationResponse = documentUploadService.validateFile(file);
            if (validationResponse.isValid()) {
                response = documentUploadService.uploadDocument(file, dto);
                return ResponseEntity.ok(response);
            } else {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege(validationResponse.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        }
        response.setResponseCode(CommonStatusCode.FAIL);
        response.setResponseMessege(validReqDto.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Get a fresh presigned URL for a previously uploaded document.
     * Call this each time the document needs to be displayed/downloaded.
     */
    @GetMapping(value = "/presigned-url/{docUploadId}")
    public ResponseEntity<?> getPresignedUrl(@PathVariable Long docUploadId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            String url = documentUploadService.getPresignedUrl(docUploadId);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Presigned URL generated");
            response.setData(url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.FAIL);
            response.setResponseMessege("Could not generate URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete a document record and its S3 file.
     */
    @DeleteMapping(value = "/{docUploadId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long docUploadId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            documentUploadService.deleteDocument(docUploadId);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Document deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.FAIL);
            response.setResponseMessege("Delete failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}

package com.cargosetu.document.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.document.service.DocumentMasterService;
import com.cargosetu.dto.DocumentMasterDTO;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = BaseURL.BASE_URL + BaseURL.DOCS_UPLOADS)
public class DocumentController {

    @Autowired
    private DocumentMasterService documentMasterService;

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

    public GenericDataDTO updateMasterDocument(DocumentMasterDTO documentMasterDTO) {
        GenericDataDTO dto = new GenericDataDTO();
        dto.setResponseCode(CommonStatusCode.SUCCESS);
        dto.setResponseMessege("Document Master Updated Successfully!");
        return dto;
    }


    public ResponseEntity<?> uploadDocument(DocumentMasterDTO documentMasterDTO) {
        GenericDataDTO dto = new GenericDataDTO();
        dto.setResponseCode(CommonStatusCode.SUCCESS);
        dto.setResponseMessege("Document Uploaded Successfully!");
        return ResponseEntity.ok(dto);
    }

}

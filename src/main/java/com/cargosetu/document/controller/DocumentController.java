package com.cargosetu.document.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.document.service.DocumentMasterService;
import com.cargosetu.dto.DocumentMasterDTO;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
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
        documentMasterService.saveDocumentMaster(documentMasterDTO);
        return dto;
    }

}

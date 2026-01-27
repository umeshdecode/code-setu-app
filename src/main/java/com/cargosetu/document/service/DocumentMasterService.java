package com.cargosetu.document.service;

import com.cargosetu.document.repository.DocumentUploadRepository;
import com.cargosetu.dto.DocumentMasterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentMasterService {
    @Autowired
    private DocumentUploadRepository documentMasterRepository;

    @Autowired
    private DocumentUploadRepository documentUploadRepository;

    public void saveDocumentMaster(DocumentMasterDTO documentMasterDTO) {

    }
}

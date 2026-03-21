package com.cargosetu.document.service;

import com.cargosetu.document.model.DocumentMaster;
import com.cargosetu.document.repository.DocumentMasterRepository;
import com.cargosetu.document.repository.DocumentUploadRepository;
import com.cargosetu.dto.DocumentMasterDTO;
import com.cargosetu.mapper.DocumentMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocumentMasterService {

    private final DocumentMasterRepository documentMasterRepository;
    private final DocumentMasterMapper documentMasterMapper;

    public boolean saveDocumentMaster(DocumentMasterDTO documentMasterDTO) {
        boolean isSaved = false;
        DocumentMaster entity = documentMasterMapper.toEntity(documentMasterDTO);
        entity = documentMasterRepository.save(entity);

        if (entity.getDocumentId() != null && entity.getDocumentId() > 0) isSaved = true;

        return isSaved;
    }
}

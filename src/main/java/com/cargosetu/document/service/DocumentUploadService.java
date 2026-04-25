package com.cargosetu.document.service;

import com.cargosetu.document.model.DocumentUpload;
import com.cargosetu.document.repository.DocumentUploadRepository;
import com.cargosetu.dto.DocumentUploadDTO;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.mapper.DocumentUploadMapper;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.utillity.FileUtility;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

@Service
public class DocumentUploadService {


    public static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String BASE_FILE_URL = "C:\\Users\\Dell\\Desktop\\Personal\\uploads";
    private final DocumentUploadMapper documentUploadMapper;
    private final DocumentUploadRepository documentUploadRepository;
    public DocumentUploadService(DocumentUploadMapper documentUploadMapper,DocumentUploadRepository documentUploadRepository) {
        this.documentUploadMapper = documentUploadMapper;
        this.documentUploadRepository = documentUploadRepository;
    }


    public GenericDataDTO uploadDocument(MultipartFile file, DocumentUploadDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            String originalName = "";
            if(file.getOriginalFilename() != null || !file.getOriginalFilename().isBlank()){
                originalName = Paths.get(file.getOriginalFilename()).getFileName().toString();
            }
            long size = file.getSize();

            String extension = "";
            if (originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".") + 1);
            }else {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("invalid file extension type");
                return response;
            }
            String filePath = FileUtility.saveMultipartFile(file, BASE_FILE_URL);

            dto.setFileName(originalName);
            dto.setFilePath(filePath);
            dto.setFileSize(size);
            dto.setFileType(extension);

            DocumentUpload entity = documentUploadMapper.toEntity(dto);

            DocumentUpload save = documentUploadRepository.save(entity);

            response.setResponseMessege("data saved successfully");
            response.setResponseCode(CommonStatusCode.SUCCESS);

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
        return response;
    }

    public ValidationResponse validateFile(MultipartFile file) {
        ValidationResponse validationResponse = new ValidationResponse(false,"fail");

        if(Objects.isNull(file)){
            validationResponse.setValid(false);
            validationResponse.setMessage("file is required!");
            return validationResponse;
        }

        if (file.isEmpty()) {
            validationResponse.setValid(false);
            validationResponse.setMessage("please upload a valid file!");
            return validationResponse;
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            validationResponse.setValid(false);
            validationResponse.setMessage("File too large, acceptable max size is "+ MAX_FILE_SIZE);
            return validationResponse;
        }

//        if (!Objects.equals(file.getContentType(), "application/pdf")) {
//            validationResponse.setValid(false);
//            validationResponse.setMessage("only pdf format is acceptable");
//            return validationResponse;
//        }
        validationResponse.setValid(true);
        validationResponse.setMessage("success");
        return validationResponse;
    }

    public ValidationResponse validateDocumentUpload(DocumentUploadDTO requestDto) {
        ValidationResponse response = new ValidationResponse(true,"success");

        if (Objects.isNull(requestDto)){
            response.setMessage("Invalid request");
            response.setValid(false);
            return response;
        }

        if(requestDto.getDocumentId() == null || requestDto.getDocumentId() <= 0 ){
            response.setMessage("Invalid document type");
            response.setValid(false);
            return response;
        }

        return response;
    }
}

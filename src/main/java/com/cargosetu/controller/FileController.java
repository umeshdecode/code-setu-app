package com.cargosetu.controller;

import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.utillity.FileUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@RequestMapping(BaseURL.BASE_URL+BaseURL.DOCUMENT)
@RestController
public class FileController {

    @Autowired
    private FileUtility fileUtility;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestParam(value = "file") MultipartFile[] file) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (file == null || file.length == 0) {
                throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"No files uploaded",null);
            }
            uploadDocuments(file);
            response.put("message", "Document Uploaded For KnowledgeBase Submitted Successfully.");
            log.info(":::::::::::::::::::::Document Uploaded For KnowledgeBase Submitted Successfully:::::::::::::::::::::");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (CustomValidationException ce) {
            log.error("Validation error while uploading document", ce);
            response.put("error", ce.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        } catch (Exception e) {
            log.error("Exception while uploading document", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void uploadDocuments( MultipartFile[] files) throws IOException {
//        String path ="";   // path from db future scope
        String path = "C:\\"; //   "C:/uploads/"
//            String fullPath = path + subFolderName;  //subFolderName in future scope
        String fullPath = path ;
        log.info(":::::File Path Generated for Upload Document ::::: " + fullPath);
        fileUtility.saveMultipleFiles(files, fullPath);
    }
}

package com.cargosetu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadResult {
    private String s3Key;
    private String fileUrl;
    private long fileSize;
}

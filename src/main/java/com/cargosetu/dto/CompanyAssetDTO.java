package com.cargosetu.dto;

import com.cargosetu.model.Company;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CompanyAssetDTO {
    private Long id;

    private Integer companyId;

    private String assetType;   // eg: LOGO , SEAL , SIGNATURE , STAMP

    private String filePath;
    private MultipartFile[] file;

}

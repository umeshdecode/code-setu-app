package com.cargosetu.dto;

import jakarta.persistence.Column;

public class CompanyAssetDTO {
    private Long id;

    private Integer companyId;

    private String assetType;   // eg: LOGO , SEAL , SIGNATURE , STAMP

    private String filePath;

}

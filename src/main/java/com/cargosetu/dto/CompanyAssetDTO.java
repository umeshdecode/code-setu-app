package com.cargosetu.dto;

import lombok.Data;

@Data
public class CompanyAssetDTO {
    private Long id;

    private Integer companyId;

    private String assetType;   // eg: LOGO , SEAL , SIGNATURE , STAMP

    private String filePath;

}

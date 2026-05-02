package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tbl_company_assets")
public class CompanyAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "asset_type", nullable = false, length = 20)
    private String assetType;   // eg: LOGO , SEAL , SIGNATURE , STAMP

    @Column(name = "file_path", nullable = false)
    private String filePath;

//    @Lob
//    @Column(name = "base64_data", columnDefinition = "LONGTEXT")
//    private String base64Data;      // full data URI
}

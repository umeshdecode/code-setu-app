package com.cargosetu.document.model;

import com.cargosetu.model.Staff;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tblm_document_upload")
public class DocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long staffDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;  // pdf, jpg, png

    @Column(nullable = false)
    private Long fileSize;

    @Column
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column
    private String remarks;
}

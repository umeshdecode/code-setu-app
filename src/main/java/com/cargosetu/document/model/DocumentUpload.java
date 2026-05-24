package com.cargosetu.document.model;

import com.cargosetu.customer.entity.Customer;
import com.cargosetu.model.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tblm_document_upload")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_upload_id")
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

    @JoinColumn(name = "customer_id" ,nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}

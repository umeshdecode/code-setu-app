package com.cargosetu.document.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "tblm_document_master",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code"})
        }
)
@Data
public class DocumentMaster extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code; // AADHAAR, PAN, DL, PASSPORT, RESUME

    @Column(nullable = false, length = 100)
    private String name; // Aadhaar Card, PAN Card

    @Column(nullable = false)
    private Boolean mandatory;  // true for DRIVER_LICENSE

    @Column(nullable = false)
    private Boolean expiryRequired; // true for license, passport

    @Column(nullable = false)
    private Boolean active = true;
}

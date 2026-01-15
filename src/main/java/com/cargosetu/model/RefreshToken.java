package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tblm_refresh_tokens")
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long staffId;

    @Column(unique = true, nullable = false, length = 500)
    private String token;

    private Instant expiresAt;

    private boolean revoked = false;
}

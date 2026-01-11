package com.cargosetu.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tblm_staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer staffId;

    private String uuid;

}

package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tblm_roles")
@Data
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rolesId;
    private String code;
    private String roleName;
    private String discription;
    private String scope;




}

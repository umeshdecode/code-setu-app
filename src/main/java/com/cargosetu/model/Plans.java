package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "tblmplans")
@Data
@EqualsAndHashCode(callSuper = false)
public class Plans extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "plan_validity")
    private LocalDate planValidity;

    @Column(name = "price")
    private Double price;

    @Column(name = "plan_category")
    private String planCategory;
}
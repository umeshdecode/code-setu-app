package com.cargosetu.customer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tblm_extra_charges")
public class ExtraCharges {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extra_charges_id")
    private Integer extraChargesId;

    @Column(name = "charge_name")
    private String chargeName;

    @Column(name = "charge_value")
    private Double chargeValue;

    @Column(name = "charge_rate")
    private Float chargeRate;
}

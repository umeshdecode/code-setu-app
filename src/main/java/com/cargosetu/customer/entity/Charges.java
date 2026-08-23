package com.cargosetu.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tblm_charges")
public class Charges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_id")
    private Long chargeId;

    @Column(name = "transport_charge")
    private Double transportCharge;

    @Column(name = "packing_charge")
    private Double packingCharge;

    @Column(name = "loading_charge")
    private Double loadingCharge;

    @Column(name = "unloading_charge")
    private Double unloadingCharge;

    @Column(name = "unpacking_charge")
    private Double unpackingCharge;

    @Column(name = "car_transport_charge")
    private Double carTransportCharge;

    @Column(name = "risk_insure_charge")
    private Double riskInsureCharge;

    @Column(name = "storage_charge")
    private Double storageCharge;

    @Column(name = "statistical_charge")
    private Double statisticalCharge;

    @Column(name = "service_charge")
    private Double serviceCharge;

    @Column(name = "service_rate")
    private Float serviceRate;

    @Column(name = "gst_rate")
    private Float gstRate;

    @Column(name = "gst_charge")
    private Double gstCharge;

    @OneToMany(cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.EAGER)
    private List<ExtraCharges> extraCharges = new ArrayList<>();

    @OneToOne(mappedBy = "charge")
    private Shifting shifting;

}

package com.cargosetu.customer.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tblm_shifting")
public class Shifting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shifting_id")
    private Long shiftingId;

    @Column(name = "shift_completed")
    private boolean isShiftCompleted = false;

    @Column(name = "shift_from_city")
    private String shiftFromCity;

    @Column(name = "shift_to_city")
    private String shiftToCity;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "shifting_type")
    private String shiftingType;

    @Column(name = "shifting_Date")
    private LocalDate shiftingStartDate;

    @Column(name = "shift_reach_date")
    private LocalDate shiftReachedDate;

    @Column(name = "shift_completed_by")
    private String shiftCompletedBy;

    @Column(name = "shift_complted_by_id")
    private Long shiftCompletedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "charge_id", referencedColumnName = "charge_id")
    private Charges charge;

    @Column(name = "shift_status")
    private String shiftStatus;

    @Column(name = "shift_actual_reach_date")
    private LocalDate shiftActualReachedDate;
    private Integer companyId;

    @Column(name = "token_money")
    private Double tokenMoney;

    // Planned-route endpoints (see trip-route.changelog.xml). Nullable for legacy
    // rows; only set once staff confirm a point - never auto-derived from a city name.
    @Column(name = "from_latitude")
    private Double fromLatitude;

    @Column(name = "from_longitude")
    private Double fromLongitude;

    @Column(name = "from_location_source")
    private String fromLocationSource;

    @Column(name = "from_location_confirmed_at")
    private LocalDateTime fromLocationConfirmedAt;

    @Column(name = "from_location_confirmed_by")
    private String fromLocationConfirmedBy;

    @Column(name = "to_latitude")
    private Double toLatitude;

    @Column(name = "to_longitude")
    private Double toLongitude;

    @Column(name = "to_location_source")
    private String toLocationSource;

    @Column(name = "to_location_confirmed_at")
    private LocalDateTime toLocationConfirmedAt;

    @Column(name = "to_location_confirmed_by")
    private String toLocationConfirmedBy;
}

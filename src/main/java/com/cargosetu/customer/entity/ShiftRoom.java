package com.cargosetu.customer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tblt_shift_room")
public class ShiftRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long roomId;

    private String roomName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shifting_id")
    private Shifting shifting;
}
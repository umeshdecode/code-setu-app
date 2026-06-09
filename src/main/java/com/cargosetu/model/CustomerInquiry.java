package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_inquiry")
@Getter
@Setter
public class CustomerInquiry extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String inquiryNumber;

    private String name;

    private String email;

    private String phone;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private InquiryStatus status;

    private String source;

}

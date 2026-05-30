package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString
@Table(name = "tblmclientservice")
public class Clientservice extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serviceid", nullable = false, length = 40)
    private Integer id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 40)
    private String value;

    @Column(name = "STAFFID", nullable = false, length = 40, updatable = false)
    private Integer staffId;

    @Column(nullable = false, length = 40)
    private String module;

}

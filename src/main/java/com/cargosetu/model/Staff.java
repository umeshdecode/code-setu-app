package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tblm_staff")
@Data
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_uuid",nullable = false,unique = true)
    private String uuid; // unique for each user or staff

    @Column(name = "username",nullable = false)
    private String userName;

    private String firstName;

    private String lastName;

    private Long mobileNumber;

    private String mobileCode;

    private String email;

    private String password;

    @ManyToMany(fetch = FetchType.LAZY)

    @JoinTable(
            name = "tblm_user_roles",
            joinColumns = @JoinColumn(name = "staff_if"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private HashSet<Roles> roles;
}

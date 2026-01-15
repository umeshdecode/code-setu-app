package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tblm_staff")
@Data
public class Staff extends AbstractModel {

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

    private String mobileNumber;

    private String mobileCode;

    private String email;

    private String password;

    @ManyToMany(fetch = FetchType.LAZY)

    @JoinTable(
            name = "tblm_user_roles",
            joinColumns = @JoinColumn(name = "staff_if"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    public Staff(){
    }

    public Staff(String uuid, String userName, String firstName, String lastName, String mobileCode, String mobileNumber, String email, String password, HashSet<Role> roles) {
        this.uuid = uuid;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobileCode = mobileCode;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
}

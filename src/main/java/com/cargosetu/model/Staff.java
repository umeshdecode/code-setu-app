package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "staff_type", length = 100)
    private String staffType; // SUPERADMIN, COMPANY_ADMIN, MANAGER, DRIVER, ACCOUNTANT

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "alternate_mobile")
    private String alternateMobile;

    @Column(name = "address_line")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "previous_password")
    private String previousPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "company_id")
    private Integer companyId;

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

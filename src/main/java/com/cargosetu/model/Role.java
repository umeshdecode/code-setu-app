package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tblm_roles")
@Data
public class Role extends AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long rolesId;
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;
    @Column(name = "description", columnDefinition = "TEXT")
    private String discription;
    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tblm_role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role(){}

    public Role(String code, String roleName, String discription, String scope) {
        this.code = code;
        this.roleName = roleName;
        this.discription = discription;
        this.scope = scope;
    }

}

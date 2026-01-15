package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "tblm_department",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"company_uuid", "dept_code"})
        }
)
@Data
public class Department extends AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer departmentId;

    @Column(name = "department_uuid", nullable = false, updatable = false)
    private String departmentUuid;

    @Column(name = "company_uuid", nullable = false)
    private String companyUuid;

    @Column(name = "dept_code", nullable = false, length = 50)
    private String code;  // ex: MARKETING_EXECUTIVE,PACKAGING,ONSITE

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department that = (Department) o;
        return departmentId != null && departmentId.equals(that.departmentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

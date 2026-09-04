package com.cargosetu.repo;

import com.cargosetu.model.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUserName(String superAdmin);

    Optional<Staff> findByEmail(String email);
    Optional<Staff> findByEmailOrUserName(String email,String username);
    Optional<Staff> findByMobileNumberAndIsDeleted(String mobileNumber, boolean isDeleted);
    Optional<Staff> findByStaffIdAndIsDeleted(Long staffId, boolean isDeleted);
    Page<Staff> findAllByCompanyIdOrderByStaffIdDesc(Integer companyId, Pageable pageable);
}

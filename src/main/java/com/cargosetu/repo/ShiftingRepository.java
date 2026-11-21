package com.cargosetu.repo;

import com.cargosetu.customer.entity.Shifting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftingRepository extends JpaRepository<Shifting, Long> {

    Page<Shifting> findAllByOrderByShiftingIdDesc(Pageable pageable);
    Page<Shifting> findAllByCompanyIdOrderByShiftingIdDesc(int company, Pageable pageable);

    @Query(value = """
    SELECT ts.* FROM tblm_shifting ts WHERE ts.shifting_id = :shiftingId
    """, nativeQuery = true)
    Optional<Shifting> findShifting(@Param("shiftingId") Long shiftingId);

    @Query("SELECT s FROM Shifting s JOIN FETCH s.customer JOIN FETCH s.charge WHERE s.shiftingId = :shiftingId")
    Optional<Shifting> findShiftingWithCustomerAndCharges(
            @Param("shiftingId") Long shiftingId);

    Page<Shifting> findByCustomer_CustomerIdOrderByShiftingIdDesc(Long customerId, Pageable pageable);

    @Query("SELECT s FROM Shifting s JOIN s.customer c WHERE " +
           "(LOWER(s.shiftFromCity) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.shiftToCity) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR c.mobileNo LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY s.shiftingId DESC")
    Page<Shifting> searchShiftings(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Shifting s JOIN s.customer c WHERE s.companyId = :companyId AND " +
           "(LOWER(s.shiftFromCity) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.shiftToCity) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR c.mobileNo LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY s.shiftingId DESC")
    Page<Shifting> searchShiftingsByCompanyId(@Param("companyId") int companyId, @Param("query") String query, Pageable pageable);
}

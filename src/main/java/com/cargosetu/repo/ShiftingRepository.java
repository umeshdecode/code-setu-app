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
    Page<Shifting> findAllByCompanyIdOrderByShiftingIdDesc(int company,Pageable pageable);

    @Query(value = """
    SELECT ts.* FROM tblm_shifting ts WHERE ts.shifting_id = :shiftingId
    """, nativeQuery = true)
    Optional<Shifting> findShifting(@Param("shiftingId") Long shiftingId);

    @Query("SELECT s FROM Shifting s JOIN FETCH s.customer JOIN FETCH s.charge WHERE s.shiftingId = :shiftingId")
    Optional<Shifting> findShiftingWithCustomerAndCharges(
            @Param("shiftingId") Long shiftingId);
}

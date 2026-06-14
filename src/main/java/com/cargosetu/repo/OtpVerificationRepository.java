package com.cargosetu.repo;

import com.cargosetu.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
    void deleteByMobileNumber(String mobileNumber);
}

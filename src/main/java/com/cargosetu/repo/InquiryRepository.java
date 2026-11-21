package com.cargosetu.repo;

import com.cargosetu.dto.InquiryRequest;
import com.cargosetu.model.CustomerInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<CustomerInquiry, Long> {
}

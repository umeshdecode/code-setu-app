package com.cargosetu.customer.repository;

import com.cargosetu.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    int countCustomersByCustomerId(Long customerId);

    Optional<Customer> findByUuidAndIsDelete(String uuid,boolean isDelete);

    Optional<Customer> findByCustomerIdAndIsDelete(Long customerId, boolean isDelete);

    Page<Customer> findByIsDelete(boolean isDelete, Pageable pageable);

    Page<Customer> findByIsDeleteOrderByCustomerIdDesc(Boolean isDelete, Pageable pageable);
    Page<Customer> findByCompanyIdAndIsDeleteOrderByCustomerIdDesc(Integer customerId, Boolean isDelete, Pageable pageable);
}

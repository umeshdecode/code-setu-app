package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.CompanyWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyWalletRepository extends JpaRepository<CompanyWallet, Long> {
    Optional<CompanyWallet> findByCompanyId(Integer companyId);
}

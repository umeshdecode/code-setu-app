package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.entity.CompanyWallet;
import com.cargosetu.marketplace.entity.LedgerEntry;
import com.cargosetu.marketplace.entity.WalletHold;
import com.cargosetu.marketplace.entity.WalletTransaction;
import com.cargosetu.marketplace.enums.PaymentMode;
import com.cargosetu.marketplace.enums.WalletTransactionType;
import com.cargosetu.marketplace.mapper.WalletTransactionMapper;
import com.cargosetu.marketplace.repository.CompanyWalletRepository;
import com.cargosetu.marketplace.repository.LedgerEntryRepository;
import com.cargosetu.marketplace.repository.WalletHoldRepository;
import com.cargosetu.marketplace.repository.WalletTransactionRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final CompanyWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletHoldRepository holdRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final WalletTransactionMapper transactionMapper;

    @Transactional
    public CompanyWallet getOrCreateWallet(Integer companyId) {
        return walletRepository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    CompanyWallet wallet = new CompanyWallet();
                    wallet.setCompanyId(companyId);
                    wallet.setAvailableBalance(BigDecimal.ZERO);
                    wallet.setHeldBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    @Transactional
    public WalletTransaction credit(Integer companyId, BigDecimal amount, String referenceType, Long referenceId, PaymentMode paymentMode, Long staffId, String description) {
        CompanyWallet wallet = getOrCreateWallet(companyId);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(wallet.getId());
        txn.setAmount(amount);
        txn.setType(WalletTransactionType.CREDIT);
        txn.setReferenceType(referenceType);
        txn.setReferenceId(referenceId);
        txn.setDescription(description);
        txn = transactionRepository.save(txn);

        LedgerEntry ledger = new LedgerEntry();
        ledger.setCompanyId(companyId);
        ledger.setCredit(amount);
        ledger.setBalanceAfter(wallet.getAvailableBalance());
        ledgerRepository.save(ledger);

        return txn;
    }

    @Transactional
    public WalletTransaction debit(Integer companyId, BigDecimal amount, String referenceType, Long referenceId, PaymentMode paymentMode, Long staffId, String description) {
        CompanyWallet wallet = getOrCreateWallet(companyId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(wallet.getId());
        txn.setAmount(amount);
        txn.setType(WalletTransactionType.DEBIT);
        txn.setReferenceType(referenceType);
        txn.setReferenceId(referenceId);
        txn.setDescription(description);
        txn = transactionRepository.save(txn);

        LedgerEntry ledger = new LedgerEntry();
        ledger.setCompanyId(companyId);
        ledger.setDebit(amount);
        ledger.setBalanceAfter(wallet.getAvailableBalance());
        ledgerRepository.save(ledger);

        return txn;
    }

    @Transactional
    public WalletHold holdAmount(Integer companyId, Long bookingId, BigDecimal amount, String reason) {
        CompanyWallet wallet = getOrCreateWallet(companyId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for hold");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
        walletRepository.save(wallet);

        WalletHold hold = new WalletHold();
        hold.setWalletId(wallet.getId());
        hold.setBookingId(bookingId);
        hold.setAmount(amount);
        hold.setStatus("ACTIVE");
        hold.setReason(reason);
        return holdRepository.save(hold);
    }

    @Transactional
    public void releaseHold(Long holdId) {
        WalletHold hold = holdRepository.findById(holdId).orElseThrow();
        if ("ACTIVE".equals(hold.getStatus())) {
            CompanyWallet wallet = walletRepository.findById(hold.getWalletId()).orElseThrow();
            wallet.setHeldBalance(wallet.getHeldBalance().subtract(hold.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(hold.getAmount()));
            walletRepository.save(wallet);

            hold.setStatus("RELEASED");
            holdRepository.save(hold);
        }
    }

    public GenericDataDTO getWalletStatement(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            CompanyWallet wallet = getOrCreateWallet(user.getCompanyId());
            response.setDataList(transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}

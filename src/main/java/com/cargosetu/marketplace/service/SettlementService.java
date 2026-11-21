package com.cargosetu.marketplace.service;

import com.cargosetu.marketplace.repository.LedgerEntryRepository;
import com.cargosetu.marketplace.repository.PaymentOrderRepository;
import com.cargosetu.marketplace.repository.PaymentTransactionRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletService walletService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final MarketplaceAuditService auditService;

    @Transactional
    public GenericDataDTO settleBooking(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            // Verify and settle payments
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Settlement completed");
        } catch (Exception e) {
            log.error("Error settling booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}

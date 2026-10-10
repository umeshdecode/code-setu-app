package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CreatePaymentOrderRequest;
import com.cargosetu.marketplace.entity.PaymentOrder;
import com.cargosetu.marketplace.entity.PaymentTransaction;
import com.cargosetu.marketplace.enums.PaymentMode;
import com.cargosetu.marketplace.enums.PaymentStatus;
import com.cargosetu.marketplace.mapper.PaymentOrderMapper;
import com.cargosetu.marketplace.mapper.PaymentTransactionMapper;
import com.cargosetu.marketplace.repository.PaymentAllocationRepository;
import com.cargosetu.marketplace.repository.PaymentOrderRepository;
import com.cargosetu.marketplace.repository.PaymentTransactionRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final WalletService walletService;
    private final MarketplaceAuditService auditService;

    @Transactional
    public GenericDataDTO createPaymentOrder(LoggedInUser user, Long bookingId, CreatePaymentOrderRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            PaymentOrder order = new PaymentOrder();
            order.setBookingId(bookingId);
            order.setAmount(request.getAmount());
            order.setStatus(PaymentStatus.PENDING);
            order.setIdempotencyKey(request.getIdempotencyKey());
            paymentOrderRepository.save(order);
            
            response.setData(paymentOrderMapper.toResponse(order));
            response.setResponseCode(CommonStatusCode.CREATED);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO processPaymentSuccess(Long orderId, String gatewayRef, String transactionRef) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            PaymentOrder order = paymentOrderRepository.findById(orderId).orElseThrow();
            order.setStatus(PaymentStatus.SUCCESS);
            paymentOrderRepository.save(order);

            PaymentTransaction txn = new PaymentTransaction();
            txn.setOrderId(order.getId());
            txn.setStatus(PaymentStatus.SUCCESS);
            txn.setGatewayRef(gatewayRef);
            txn.setTransactionRef(transactionRef);
            paymentTransactionRepository.save(txn);

            // Optional: trigger wallet updates
            // walletService.credit(...)

            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO processPaymentFailure(Long orderId, String gatewayRef, String reason) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            PaymentOrder order = paymentOrderRepository.findById(orderId).orElseThrow();
            order.setStatus(PaymentStatus.FAILED);
            paymentOrderRepository.save(order);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO processRefund(LoggedInUser user, Long orderId, BigDecimal refundAmount) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getPaymentHistory(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(paymentOrderRepository.findByBookingId(bookingId));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO handleWebhook(Map<String, Object> payload) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    public GenericDataDTO getWalletStatement(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }
}

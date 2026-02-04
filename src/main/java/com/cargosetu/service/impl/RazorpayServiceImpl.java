package com.cargosetu.service.impl;

import com.cargosetu.dto.RazorpayConfig;
import com.cargosetu.service.RazorpayService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private RazorpayConfig config;

    @Override
    public JSONObject  createOrder(int amount) throws RazorpayException {
        RazorpayClient client =
                new RazorpayClient(config.getId(), config.getSecret());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // ₹ → paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        return client.orders.create(orderRequest).toJson();
    }

    @Override
    public boolean verifySignature(
            String orderId,
            String paymentId,
            String signature) {

        String payload = orderId + "|" + paymentId;

        try {
            return Utils.verifySignature(
                    payload,
                    signature,
                    config.getSecret()
            );
        } catch (Exception e) {
            return false;
        }
    }

}

package com.cargosetu.service;

import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public interface RazorpayService {
    public JSONObject createOrder(int amount) throws RazorpayException;


    public boolean verifySignature(
            String orderId,
            String paymentId,
            String signature);
}

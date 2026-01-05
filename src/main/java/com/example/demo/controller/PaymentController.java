package com.example.demo.controller;

import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;
import com.example.demo.service.PaymentServiceV2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;
    private final PaymentServiceV2 serviceV2;

    public PaymentController(PaymentService service, PaymentServiceV2 serviceV2) {
        this.service = service;
        this.serviceV2 = serviceV2;
    }

    @PostMapping
    public Payment pay(
            @RequestHeader("Idempotency-Key") String key,
            @RequestParam Long userId,
            @RequestParam String orderId,
            @RequestParam Long amount
    ) {
        return serviceV2.pay(userId, orderId, amount, key);
    }
}

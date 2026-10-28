package com.keianluxandor.backend.payment;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account/orders/{orderNumber}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // "Pay now": creates a Razorpay order and returns what Razorpay Checkout needs
    @PostMapping
    public StartPaymentResponse startPayment(@AuthenticationPrincipal AuthUser user,
                                             @PathVariable("orderNumber") String orderNumber) {
        return paymentService.startPayment(user, orderNumber);
    }

    // Called by the website after Razorpay Checkout reports success
    @PostMapping("/verify")
    public PaymentResultResponse verifyPayment(@AuthenticationPrincipal AuthUser user,
                                               @PathVariable("orderNumber") String orderNumber,
                                               @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyPayment(user.id(), orderNumber, request);
    }
}
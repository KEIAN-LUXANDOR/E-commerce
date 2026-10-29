package com.keianluxandor.backend.payment;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Called by Razorpay's servers (not by the website)
@RestController
@RequestMapping("/api/v1/payments/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayClient razorpay;
    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayClient razorpay, RazorpayWebhookService webhookService) {
        this.razorpay = razorpay;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestBody byte[] body,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(name = "X-Razorpay-Event-Id", required = false) String eventId) {

        // The signature is checked on the RAW body, exactly as Razorpay sent it
        String rawBody = new String(body, StandardCharsets.UTF_8);
        if (!razorpay.isValidWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Razorpay webhook: invalid signature (or RAZORPAY_WEBHOOK_SECRET is not set)");
            return ResponseEntity.badRequest().build();
        }

        webhookService.handle(eventId, rawBody);
        return ResponseEntity.ok().build();
    }
}
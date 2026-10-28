package com.keianluxandor.backend.payment;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RazorpayClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayClient.class);
    private static final String API_BASE_URL = "https://api.razorpay.com/v1";

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient restClient;

    public RazorpayClient(@Value("${app.razorpay.key-id:}") String keyId,
                          @Value("${app.razorpay.key-secret:}") String keySecret,
                          @Value("${app.razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId = keyId.trim();
        this.keySecret = keySecret.trim();
        this.webhookSecret = webhookSecret.trim();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(API_BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBasicAuth(this.keyId, this.keySecret))
                .build();

        // Never log the keys themselves
        if (!isConfigured()) {
            log.warn("Razorpay keys are not set: online payment is switched off.");
        } else {
            log.info("Razorpay is configured in {} mode.", this.keyId.startsWith("rzp_live_") ? "LIVE" : "TEST");
        }
    }

    public boolean isConfigured() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    // Safe to send to the browser (Razorpay Checkout needs it). The secret is never sent.
    public String keyId() {
        return keyId;
    }

    // Creates a Razorpay order and returns its id, e.g. "order_Pa1b2C3d4E5f6G"
    public String createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes) {
        Map<String, Object> response = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", amountPaise,
                        "currency", currency,
                        "receipt", receipt,
                        "notes", notes))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });

        Object id = (response == null) ? null : response.get("id");
        if (!(id instanceof String orderId) || orderId.isBlank()) {
            throw new IllegalStateException("Razorpay did not return an order id");
        }
        return orderId;
    }

    // Checkout success: signature = HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, key secret)
    public boolean isValidPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        if (!isConfigured()) {
            return false;
        }
        return sameSignature(hmacSha256Hex(razorpayOrderId + "|" + razorpayPaymentId, keySecret), signature);
    }

    // Webhook: signature = HMAC-SHA256(raw request body, webhook secret)  (used in Part D2)
    public boolean isValidWebhookSignature(String rawBody, String signature) {
        if (webhookSecret.isBlank()) {
            return false;
        }
        return sameSignature(hmacSha256Hex(rawBody, webhookSecret), signature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    // Constant-time comparison, so an attacker can't guess the signature character by character
    private static boolean sameSignature(String expectedHex, String given) {
        if (given == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                given.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
}
package com.keianluxandor.backend.payment;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final WebhookEventRepository eventRepository;
    private final PaymentService paymentService;

    public RazorpayWebhookService(WebhookEventRepository eventRepository, PaymentService paymentService) {
        this.eventRepository = eventRepository;
        this.paymentService = paymentService;
    }

    // One transaction: if processing fails, the event is NOT marked as done, so Razorpay's retry works
    @Transactional
    public void handle(String eventIdHeader, String rawBody) {
        Map<String, Object> event;
        try {
            event = new BasicJsonParser().parseMap(rawBody);
        } catch (RuntimeException e) {
            log.warn("Razorpay webhook body is not valid JSON");
            return;
        }

        String type = text(event, "event");
        if (type == null) {
            log.warn("Razorpay webhook without an event type");
            return;
        }
        String paymentId = text(event, "payload", "payment", "entity", "id");
        String razorpayOrderId = text(event, "payload", "payment", "entity", "order_id");
        Long amountPaise = number(event, "payload", "payment", "entity", "amount");

        // Razorpay may deliver the same event more than once: handle each event only once
        String eventKey = (eventIdHeader != null && !eventIdHeader.isBlank())
                ? eventIdHeader.trim()
                : type + ":" + paymentId;
        if (eventRepository.recordIfNew(cut(eventKey, 100), cut(type, 60)) == 0) {
            log.info("Razorpay webhook {} was already processed", eventKey);
            return;
        }

        switch (type) {
            case "payment.captured", "order.paid" -> {
                if (paymentId == null || razorpayOrderId == null || amountPaise == null) {
                    log.warn("Razorpay webhook {} is missing payment details", type);
                    return;
                }
                paymentService.handleCapturedPayment(razorpayOrderId, paymentId, amountPaise);
            }
            case "payment.failed" -> log.info("Razorpay payment failed for {}: {}", razorpayOrderId,
                    text(event, "payload", "payment", "entity", "error_description"));
            default -> log.info("Ignored Razorpay webhook event {}", type);
        }
    }

    // Walks into nested JSON objects, e.g. at(event, "payload", "payment", "entity", "id")
    private static Object at(Map<String, Object> root, String... path) {
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    private static String text(Map<String, Object> root, String... path) {
        Object value = at(root, path);
        return (value instanceof String s && !s.isBlank()) ? s : null;
    }

    private static Long number(Map<String, Object> root, String... path) {
        Object value = at(root, path);
        return (value instanceof Number n) ? n.longValue() : null;
    }

    private static String cut(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
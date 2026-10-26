package com.keianluxandor.backend.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Every minute: cancels unpaid orders whose payment time is over, and puts the diamonds back on sale
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderExpiryJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.orders.expiry-check-ms:60000}", initialDelay = 30000)
    public void cancelExpiredOrders() {
        List<UUID> expired = orderRepository.findIdsExpiredBefore(OrderStatus.PENDING_PAYMENT, Instant.now());

        for (UUID orderId : expired) {
            try {
                if (orderService.expireIfUnpaid(orderId)) {
                    log.info("Cancelled unpaid order {}: payment time ran out", orderId);
                }
            } catch (RuntimeException e) {
                // One bad order must not stop the others
                log.error("Could not cancel expired order {}", orderId, e);
            }
        }
    }
}
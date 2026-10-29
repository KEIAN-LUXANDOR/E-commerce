package com.keianluxandor.backend.payment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.auth.AuthUser;
import com.keianluxandor.backend.common.ConflictException;
import com.keianluxandor.backend.common.NotFoundException;
import com.keianluxandor.backend.order.CustomerOrder;
import com.keianluxandor.backend.order.OrderEmailService;
import com.keianluxandor.backend.order.OrderRepository;
import com.keianluxandor.backend.order.OrderService;
import com.keianluxandor.backend.order.OrderStatus;
import com.keianluxandor.backend.order.PaymentOutcome;
import com.keianluxandor.backend.order.ShippingAddress;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_ATTEMPTS = 10;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderEmailService orderEmailService;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpay;

    public PaymentService(OrderRepository orderRepository,
                          OrderService orderService,
                          OrderEmailService orderEmailService,
                          PaymentRepository paymentRepository,
                          RazorpayClient razorpay) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.orderEmailService = orderEmailService;
        this.paymentRepository = paymentRepository;
        this.razorpay = razorpay;
    }

    // Creates a new Razorpay order for this payment attempt.
    // Deliberately NOT one big transaction: no database lock is held while we wait for Razorpay.
    public StartPaymentResponse startPayment(AuthUser user, String orderNumber) {
        if (!razorpay.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payment is not available right now. Please try again later.");
        }

        CustomerOrder order = findOrder(user.id(), orderNumber);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("This order is already paid.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("This order can no longer be paid.");
        }
        if (!order.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("The time to pay for this order has run out. Please place the order again.");
        }

        long attempts = paymentRepository.countByOrderId(order.getId());
        if (attempts >= MAX_ATTEMPTS) {
            throw new ConflictException("Too many payment attempts for this order. Please cancel it and order again.");
        }
        int attemptNumber = (int) attempts + 1;

        String razorpayOrderId;
        try {
            razorpayOrderId = razorpay.createOrder(
                    order.getTotalPaise(),
                    order.getCurrency(),
                    order.getOrderNumber() + "-" + attemptNumber,   // receipt, must be unique at Razorpay
                    Map.of("orderNumber", order.getOrderNumber()));
        } catch (RuntimeException e) {
            log.error("Could not create Razorpay order for {}: {}", order.getOrderNumber(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment could not be started. Please try again.");
        }

        try {
            paymentRepository.save(new Payment(order.getId(), attemptNumber, razorpayOrderId,
                    order.getTotalPaise(), order.getCurrency()));
        } catch (DataIntegrityViolationException e) {
            // "Pay" clicked twice at the same moment
            throw new ConflictException("Payment is already being started. Please try again.");
        }

        ShippingAddress ship = order.getShippingAddress();
        return new StartPaymentResponse(
                razorpay.keyId(),
                razorpayOrderId,
                order.getTotalPaise(),
                order.getCurrency(),
                order.getOrderNumber(),
                order.getExpiresAt(),
                ship.fullName(),
                user.email(),
                "+91" + ship.phone());
    }

    // Checkout success callback from the website
    @Transactional
    public PaymentResultResponse verifyPayment(UUID userId, String orderNumber, VerifyPaymentRequest request) {
        CustomerOrder order = findOrder(userId, orderNumber);

        Payment payment = paymentRepository.findByProviderOrderIdForUpdate(request.razorpayOrderId())
                .filter(p -> p.getOrderId().equals(order.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "These payment details don't belong to this order."));

        // The signature is checked with the Razorpay order id from OUR database, not the browser's
        if (!razorpay.isValidPaymentSignature(payment.getProviderOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature())) {
            log.warn("Invalid payment signature for order {}", orderNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment could not be verified.");
        }

        PaymentStatus status = recordSuccessfulPayment(payment, request.razorpayPaymentId());
        return PaymentResultResponse.of(orderNumber, status);
    }

    // Webhook from Razorpay: a payment was captured (the signature was already checked)
    @Transactional
    public void handleCapturedPayment(String razorpayOrderId, String razorpayPaymentId, long amountPaise) {
        Payment payment = paymentRepository.findByProviderOrderIdForUpdate(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook for a Razorpay order we don't know: {}", razorpayOrderId);
            return;
        }
        if (payment.getAmountPaise() != amountPaise) {
            log.error("Webhook amount {} does not match expected {} for Razorpay order {}",
                    amountPaise, payment.getAmountPaise(), razorpayOrderId);
            return;
        }
        recordSuccessfulPayment(payment, razorpayPaymentId);
    }

    // Shared by the checkout callback and the webhook. The payment row must already be locked.
    PaymentStatus recordSuccessfulPayment(Payment payment, String razorpayPaymentId) {
        if (payment.getStatus() != PaymentStatus.CREATED) {
            return payment.getStatus(); // already recorded by the other path: nothing to do
        }

        PaymentOutcome outcome = orderService.confirmPayment(payment.getOrderId());

        // Checked AFTER confirmPayment, which holds the order lock, so we see any other finished payment
        boolean anotherPaymentPaid = paymentRepository.existsByOrderIdAndStatusAndIdNot(
                payment.getOrderId(), PaymentStatus.PAID, payment.getId());

        if (outcome == PaymentOutcome.NEEDS_REFUND
                || (outcome == PaymentOutcome.ALREADY_PAID && anotherPaymentPaid)) {
            payment.markRefundRequired(razorpayPaymentId);
            log.warn("Payment {} for order {} must be refunded ({})", razorpayPaymentId, payment.getOrderId(), outcome);
            orderEmailService.sendRefundNotice(payment.getOrderId(), payment.getAmountPaise());
        } else {
            payment.markPaid(razorpayPaymentId);
            log.info("Payment {} received for order {}", razorpayPaymentId, payment.getOrderId());
            if (outcome == PaymentOutcome.PAID) {
                orderEmailService.sendOrderConfirmed(payment.getOrderId());
            }
        }
        return payment.getStatus();
    }

    private CustomerOrder findOrder(UUID userId, String orderNumber) {
        return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new NotFoundException("Order not found."));
    }
}
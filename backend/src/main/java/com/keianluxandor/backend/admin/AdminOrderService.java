package com.keianluxandor.backend.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.common.NotFoundException;
import com.keianluxandor.backend.common.PageResponse;
import com.keianluxandor.backend.order.CustomerOrder;
import com.keianluxandor.backend.order.OrderItem;
import com.keianluxandor.backend.order.OrderItemResponse;
import com.keianluxandor.backend.order.OrderStatus;
import com.keianluxandor.backend.payment.PaymentRepository;
import com.keianluxandor.backend.payment.PaymentStatus;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class AdminOrderService {

    // Dates in the filter are Indian dates (a day starts at 00:00 IST)
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public AdminOrderService(AdminOrderRepository orderRepository,
                             UserRepository userRepository,
                             PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummary> listOrders(OrderStatus status, String q,
                                                      LocalDate from, LocalDate to,
                                                      int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));

        Page<CustomerOrder> result = orderRepository.findAll(
                filter(status, q, from, to), PageRequest.of(safePage, safeSize, newestFirst));

        // Load all customers of this page in one query
        List<UUID> userIds = result.getContent().stream().map(CustomerOrder::getUserId).distinct().toList();
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AdminOrderSummary> rows = result.getContent().stream()
                .map(order -> {
                    User user = users.get(order.getUserId());
                    List<OrderItem> items = order.getItems();
                    return new AdminOrderSummary(
                            order.getOrderNumber(),
                            order.getStatus(),
                            user == null ? null : user.getFullName(),
                            user == null ? null : user.getEmail(),
                            items.size(),
                            items.isEmpty() ? null : items.get(0).getProductName(),
                            order.getTotalPaise(),
                            order.getCreatedAt(),
                            order.getPaidAt());
                })
                .toList();

        return new PageResponse<>(rows, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail getOrder(String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Order not found."));

        AdminOrderDetail.Customer customer = userRepository.findById(order.getUserId())
                .map(user -> new AdminOrderDetail.Customer(
                        user.getId(), user.getFullName(), user.getEmail(), user.getPhone()))
                .orElse(null);

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getLineNumber(),
                        item.getProductSlug(),
                        item.getProductName(),
                        item.getSku(),
                        item.getDescription(),
                        item.getImageUrl(),
                        item.getPricePaise()))
                .toList();

        List<AdminOrderDetail.PaymentAttempt> payments = paymentRepository
                .findByOrderIdOrderByAttemptNumberAsc(order.getId()).stream()
                .map(p -> new AdminOrderDetail.PaymentAttempt(
                        p.getAttemptNumber(),
                        p.getProviderOrderId(),
                        p.getProviderPaymentId(),
                        p.getAmountPaise(),
                        p.getStatus(),
                        p.getCreatedAt()))
                .toList();

        return new AdminOrderDetail(
                order.getOrderNumber(),
                order.getStatus(),
                customer,
                order.getSubtotalPaise(),
                order.getShippingPaise(),
                order.getDiscountPaise(),
                order.getGstPaise(),
                order.getTotalPaise(),
                order.getCurrency(),
                order.getShippingAddress(),
                items,
                payments,
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCancelReason());
    }

    @Transactional(readOnly = true)
    public AdminOrderStats stats() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : orderRepository.countOrdersPerStatus()) {
            counts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }
        return new AdminOrderStats(counts, paymentRepository.countByStatus(PaymentStatus.REFUND_REQUIRED));
    }

    // Builds the WHERE part from the filters that were given
    private static Specification<CustomerOrder> filter(OrderStatus status, String q, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                Instant start = from.atStartOfDay(INDIA).toInstant();
                conditions.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), start));
            }
            if (to != null) {
                Instant endExclusive = to.plusDays(1).atStartOfDay(INDIA).toInstant();
                conditions.add(cb.lessThan(root.<Instant>get("createdAt"), endExclusive));
            }
            if (q != null && !q.isBlank()) {
                String text = q.trim();
                if (text.length() > 100) {
                    text = text.substring(0, 100);
                }
                if (text.toUpperCase(Locale.ROOT).matches("KL\\d+")) {
                    // Looks like an order number: exact match
                    conditions.add(cb.equal(root.get("orderNumber"), text.toUpperCase(Locale.ROOT)));
                } else {
                    // Otherwise: orders of customers whose email contains the text
                    Subquery<UUID> matchingUsers = query.subquery(UUID.class);
                    Root<User> user = matchingUsers.from(User.class);
                    matchingUsers.select(user.<UUID>get("id"))
                            .where(cb.like(cb.lower(user.get("email")),
                                    "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%", '\\'));
                    conditions.add(root.get("userId").in(matchingUsers));
                }
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    // So that % and _ typed by the admin are searched as normal characters
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
package com.keianluxandor.backend.order;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.address.Address;
import com.keianluxandor.backend.address.AddressRepository;
import com.keianluxandor.backend.cart.CartLineResponse;
import com.keianluxandor.backend.cart.CartResponse;
import com.keianluxandor.backend.cart.CartService;
import com.keianluxandor.backend.catalog.DiamondResponse;
import com.keianluxandor.backend.catalog.ProductResponse;
import com.keianluxandor.backend.catalog.ProductVariantResponse;
import com.keianluxandor.backend.catalog.StockService;
import com.keianluxandor.backend.common.ConflictException;
import com.keianluxandor.backend.common.NotFoundException;
import com.keianluxandor.backend.common.PageResponse;
import com.keianluxandor.backend.user.EmailVerificationGuard;

@Service
public class OrderService {

    // How long the customer has to pay before the diamonds go back on sale
    static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final StockService stockService;
    private final AddressRepository addressRepository;
    private final EmailVerificationGuard emailVerificationGuard;

    public OrderService(OrderRepository orderRepository,
                        CartService cartService,
                        StockService stockService,
                        AddressRepository addressRepository,
                        EmailVerificationGuard emailVerificationGuard) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.stockService = stockService;
        this.addressRepository = addressRepository;
        this.emailVerificationGuard = emailVerificationGuard;
    }

    // Creates an order from the cart and reserves the diamonds.
    // Everything happens in one transaction: if any step fails, nothing is saved or reserved.
    @Transactional
    public OrderResponse placeOrder(UUID userId, UUID addressId) {
        emailVerificationGuard.requireVerifiedEmail(userId);

        orderRepository.findFirstByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT).ifPresent(pending -> {
            throw new ConflictException("You already have an unpaid order (" + pending.getOrderNumber()
                    + "). Please pay for it or cancel it first.");
        });

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found."));

        CartResponse cart = cartService.getCart(userId);
        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty.");
        }
        if (cart.hasUnavailableItems()) {
            throw new ConflictException(
                    "Some diamonds in your cart have been sold. Please remove them and try again.");
        }

        CustomerOrder order = new CustomerOrder(
                "KL" + orderRepository.nextOrderNumber(),
                userId,
                toShippingAddress(address),
                Instant.now().plus(PAYMENT_WINDOW));

        for (CartLineResponse line : cart.items()) {
            ProductResponse product = line.product();

            // Reserve in the database. If someone else bought it a moment ago, the whole order is undone.
            if (!stockService.reserveOne(line.variantId())) {
                throw new ConflictException("Sorry, " + product.name()
                        + " has just been sold. Please remove it from your cart.");
            }

            order.addItem(
                    product.id(),
                    line.variantId(),
                    product.slug(),
                    product.name(),
                    skuOf(product, line.variantId()),
                    summaryOf(product.diamond()),
                    firstImageOf(product),
                    line.pricePaise());
        }

        try {
            orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            // Two "Place order" clicks at the same moment: the database allows only one unpaid order
            throw new ConflictException("You already have an unpaid order. Please pay for it or cancel it first.");
        }

        cart.items().forEach(line -> cartService.removeItem(userId, line.variantId()));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<CustomerOrder> result = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));

        return new PageResponse<>(
                result.getContent().stream().map(OrderResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, String orderNumber) {
        return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order not found."));
    }

    // Customer cancels an unpaid order: the diamonds go back on sale
    @Transactional
    public OrderResponse cancelOrder(UUID userId, String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumberAndUserIdForUpdate(orderNumber, userId)
                .orElseThrow(() -> new NotFoundException("Order not found."));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only unpaid orders can be cancelled.");
        }

        cancelAndRelease(order, "CUSTOMER_CANCELLED");
        return OrderResponse.from(order);
    }

    // Puts every diamond back on sale and marks the order CANCELLED.
    // Used by the expiry job: cancels the order only if it is still unpaid and its time is up.
    // The row lock means a payment arriving at the same moment can't clash with this.
    @Transactional
    public boolean expireIfUnpaid(UUID orderId) {
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null
                || order.getStatus() != OrderStatus.PENDING_PAYMENT
                || order.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        cancelAndRelease(order, "PAYMENT_TIMEOUT");
        return true;
    }
    // Also used by the automatic expiry (Part C).
    void cancelAndRelease(CustomerOrder order, String reason) {
        order.getItems().stream()
                .map(OrderItem::getVariantId)
                .filter(Objects::nonNull)
                .forEach(stockService::releaseOne);
        order.cancel(reason, Instant.now());
    }

    private static ShippingAddress toShippingAddress(Address address) {
        return new ShippingAddress(
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPincode());
    }

    private static String skuOf(ProductResponse product, UUID variantId) {
        return product.variants().stream()
                .filter(variant -> variant.id().equals(variantId))
                .map(ProductVariantResponse::sku)
                .findFirst()
                .orElse("UNKNOWN");
    }

    // e.g. "2.01 ct, F, VS1, IGI"
    private static String summaryOf(DiamondResponse diamond) {
        if (diamond == null) {
            return null;
        }
        return diamond.carat() + " ct, " + diamond.color() + ", " + diamond.clarity() + ", " + diamond.lab();
    }

    private static String firstImageOf(ProductResponse product) {
        return product.images().isEmpty() ? null : product.images().get(0).url();
    }
}
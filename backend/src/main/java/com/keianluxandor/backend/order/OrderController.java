package com.keianluxandor.backend.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;
import com.keianluxandor.backend.common.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal AuthUser user,
                                    @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(user.id(), request.addressId());
    }

    @GetMapping
    public PageResponse<OrderResponse> listOrders(@AuthenticationPrincipal AuthUser user,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        return orderService.listOrders(user.id(), page, size);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse getOrder(@AuthenticationPrincipal AuthUser user,
                                  @PathVariable("orderNumber") String orderNumber) {
        return orderService.getOrder(user.id(), orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal AuthUser user,
                                     @PathVariable("orderNumber") String orderNumber) {
        return orderService.cancelOrder(user.id(), orderNumber);
    }
}
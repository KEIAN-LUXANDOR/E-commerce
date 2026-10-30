package com.keianluxandor.backend.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.common.PageResponse;
import com.keianluxandor.backend.order.OrderStatus;

// ADMIN only: SecurityConfig protects everything under /api/v1/admin/**
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    // e.g. /api/v1/admin/orders?status=PAID&q=riya&from=2026-10-01&to=2026-10-31&page=0&size=20
    @GetMapping
    public PageResponse<AdminOrderSummary> listOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminOrderService.listOrders(status, q, from, to, page, size);
    }

    @GetMapping("/stats")
    public AdminOrderStats stats() {
        return adminOrderService.stats();
    }

    @GetMapping("/{orderNumber}")
    public AdminOrderDetail getOrder(@PathVariable("orderNumber") String orderNumber) {
        return adminOrderService.getOrder(orderNumber);
    }
}
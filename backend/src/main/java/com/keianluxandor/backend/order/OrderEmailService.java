package com.keianluxandor.backend.order;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.keianluxandor.backend.mail.EmailService;
import com.keianluxandor.backend.user.UserRepository;

// Emails about orders. Call these inside the payment transaction:
// the email is sent only after the transaction is saved, so we never email about a change that was undone.
@Service
public class OrderEmailService {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter
            .ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"));

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public OrderEmailService(OrderRepository orderRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public void sendOrderConfirmed(UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        String currency = order.getCurrency();
        userRepository.findById(order.getUserId()).ifPresent(user -> {
            StringBuilder text = new StringBuilder();
            text.append("Hello ").append(user.getFullName()).append(",\n\n")
                    .append("Thank you! We have received your payment and your order is confirmed.\n\n")
                    .append("Order number: ").append(order.getOrderNumber()).append('\n');
            if (order.getPaidAt() != null) {
                text.append("Paid on: ").append(UTC_TIME.format(order.getPaidAt())).append(" (UTC)\n");
            }
            text.append("\nItems:\n");
            for (OrderItem item : order.getItems()) {
                text.append("  ").append(item.getLineNumber()).append(". ").append(item.getProductName());
                if (item.getDescription() != null) {
                    text.append(" (").append(item.getDescription()).append(')');
                }
                text.append(" - ").append(money(item.getPricePaise(), currency)).append('\n');
            }
            text.append("\nTotal paid: ").append(money(order.getTotalPaise(), currency)).append("\n\n")
                    .append("Delivery address:\n").append(address(order.getShippingAddress())).append('\n')
                    .append("We will email you again when your order is shipped.\n")
                    .append("View your order: ").append(frontendUrl).append("/orders/")
                    .append(order.getOrderNumber()).append("\n\n")
                    .append("Keian Luxandor");

            sendAfterCommit(user.getEmail(), "Order " + order.getOrderNumber() + " confirmed", text.toString());
        });
    }

    public void sendRefundNotice(UUID orderId, long amountMinor) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        userRepository.findById(order.getUserId()).ifPresent(user -> {
            String text = "Hello " + user.getFullName() + ",\n\n"
                    + "We received your payment of " + money(amountMinor, order.getCurrency()) + " for order "
                    + order.getOrderNumber() + ".\n\n"
                    + "Unfortunately the payment arrived after the order had expired, and a diamond in it "
                    + "was bought by another customer in the meantime, so we cannot complete this order.\n\n"
                    + "We will refund the full amount to your original payment method and email you "
                    + "when the refund is done. We are sorry for the trouble.\n\n"
                    + "Keian Luxandor";
            sendAfterCommit(user.getEmail(), "Payment received for order " + order.getOrderNumber()
                    + " - refund on its way", text);
        });
    }

   // The customer paid twice for the same order (e.g. two browser tabs). The first payment counts.
    public void sendDuplicatePaymentNotice(UUID orderId, long amountMinor) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        userRepository.findById(order.getUserId()).ifPresent(user -> {
            String text = "Hello " + user.getFullName() + ",\n\n"
                    + "Your order " + order.getOrderNumber() + " is already paid and confirmed.\n\n"
                    + "We also received a second payment of " + money(amountMinor, order.getCurrency())
                    + " for the same order, probably because the payment was made twice.\n\n"
                    + "We will refund this second payment in full to your original payment method and "
                    + "email you when the refund is done. You don't need to do anything.\n\n"
                    + "Keian Luxandor";
            sendAfterCommit(user.getEmail(), "Second payment for order " + order.getOrderNumber()
                    + " - refund on its way", text);
        });
    }
    // Amount in the smallest unit -> text.
    // 34800 + USD -> "$348.00";  3070000 + INR -> "Rs. 30,700.00" (Indian grouping)
    static String money(long minorUnits, String currency) {
        if ("INR".equals(currency)) {
            return "Rs. " + indianGrouping(minorUnits);
        }
        String symbol = switch (currency) {
            case "USD" -> "$";
            case "EUR" -> "EUR ";
            case "GBP" -> "GBP ";
            default -> currency + " ";
        };
        return symbol + String.format(Locale.US, "%,.2f", BigDecimal.valueOf(minorUnits, 2));
    }

    // 18500000 -> "1,85,000.00": last 3 digits, then groups of 2
    private static String indianGrouping(long minorUnits) {
        String digits = Long.toString(minorUnits / 100);
        long fraction = minorUnits % 100;
        String grouped;
        if (digits.length() <= 3) {
            grouped = digits;
        } else {
            String lastThree = digits.substring(digits.length() - 3);
            String rest = digits.substring(0, digits.length() - 3);
            StringBuilder pairs = new StringBuilder();
            for (int end = rest.length(); end > 0; end -= 2) {
                int start = Math.max(0, end - 2);
                pairs.insert(0, rest.substring(start, end));
                if (start > 0) {
                    pairs.insert(0, ',');
                }
            }
            grouped = pairs + "," + lastThree;
        }
        return grouped + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    private static String address(ShippingAddress a) {
        StringBuilder text = new StringBuilder();
        text.append("  ").append(a.fullName()).append(", ").append(a.phone()).append('\n');
        text.append("  ").append(a.line1()).append('\n');
        if (a.line2() != null) {
            text.append("  ").append(a.line2()).append('\n');
        }
        if (a.landmark() != null) {
            text.append("  ").append(a.landmark()).append('\n');
        }
        text.append("  ").append(a.city()).append(", ").append(a.state()).append(' ').append(a.pincode()).append('\n');
        return text.toString();
    }

    private void sendAfterCommit(String to, String subject, String text) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.send(to, subject, text);
                }
            });
        } else {
            emailService.send(to, subject, text);
        }
    }
}
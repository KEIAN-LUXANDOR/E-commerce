package com.keianluxandor.backend.contact;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.common.PageResponse;

// Admin dashboard: contact messages. Everything under /api/v1/admin/** is ADMIN-only already.
@RestController
@RequestMapping("/api/v1/admin/contact-messages")
public class AdminContactController {

    private final ContactService contactService;

    public AdminContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // ?status=NEW for only unanswered messages; newest first
    @GetMapping
    public PageResponse<ContactMessageResponse> list(@RequestParam(required = false) ContactMessage.Status status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return contactService.list(status, page, size);
    }

    // Badge on the dashboard: number of new messages
    @GetMapping("/stats")
    public ContactStats stats() {
        return contactService.stats();
    }

    @GetMapping("/{id}")
    public ContactMessageResponse get(@PathVariable UUID id) {
        return contactService.get(id);
    }

    @PutMapping("/{id}/handled")
    public ContactMessageResponse markHandled(@PathVariable UUID id) {
        return contactService.markHandled(id);
    }
}
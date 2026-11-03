package com.keianluxandor.backend.contact;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.auth.EmailRateLimiter;
import com.keianluxandor.backend.common.NotFoundException;
import com.keianluxandor.backend.common.PageResponse;

// "Contact us" messages: saved in the database and shown on the admin dashboard (no emails are sent)
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ContactMessageRepository repository;
    private final EmailRateLimiter rateLimiter;

    public ContactService(ContactMessageRepository repository, EmailRateLimiter rateLimiter) {
        this.repository = repository;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void submit(ContactRequest request, UUID userId) {
        if (request.website() != null && !request.website().isBlank()) {
            // A bot filled in the hidden field: answer "OK" so it doesn't retry, but don't save anything
            log.info("Contact form: ignored a message with the hidden field filled in");
            return;
        }
        // Spam protection: at most one message per email address per minute
        if (!rateLimiter.tryAcquire("contact:" + request.email())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have just sent us a message. Please wait a minute before sending another.");
        }
        ContactMessage saved = repository.save(new ContactMessage(userId, request));
        log.info("Contact message {} received", saved.getId());   // the message text is never logged
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> list(ContactMessage.Status status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<ContactMessage> result = status == null
                ? repository.findAllByOrderByCreatedAtDesc(pageRequest)
                : repository.findByStatusOrderByCreatedAtDesc(status, pageRequest);

        return new PageResponse<>(
                result.getContent().stream().map(ContactMessageResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse get(UUID id) {
        return ContactMessageResponse.from(find(id));
    }

    // The admin has answered the customer (by email or phone, outside the website)
    @Transactional
    public ContactMessageResponse markHandled(UUID id) {
        ContactMessage message = find(id);
        message.markHandled();
        return ContactMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public ContactStats stats() {
        return new ContactStats(repository.countByStatus(ContactMessage.Status.NEW), repository.count());
    }

    private ContactMessage find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Message not found."));
    }
}
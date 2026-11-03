package com.keianluxandor.backend.contact;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;
import com.keianluxandor.backend.auth.MessageResponse;

import jakarta.validation.Valid;

// The "Contact us" form. No login needed; if the customer is logged in, the message is linked to their account.
@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse submit(@AuthenticationPrincipal AuthUser user,
                                  @Valid @RequestBody ContactRequest request) {
        contactService.submit(request, user == null ? null : user.id());
        return new MessageResponse("Thank you! We have received your message and will get back to you soon.");
    }
}
package com.keianluxandor.backend.profile;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;
import com.keianluxandor.backend.auth.MessageResponse;
import com.keianluxandor.backend.auth.UserResponse;
import com.keianluxandor.backend.user.EmailVerificationGuard;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// The customer is always taken from the login session, never from the request
@RestController
@RequestMapping("/api/v1/account")
public class ProfileController {

    private final ProfileService profileService;
    private final EmailVerificationGuard emailVerificationGuard;

    public ProfileController(ProfileService profileService, EmailVerificationGuard emailVerificationGuard) {
        this.profileService = profileService;
        this.emailVerificationGuard = emailVerificationGuard;
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.id(), request);
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser user,
                                          @Valid @RequestBody ChangePasswordRequest body,
                                          HttpServletRequest request) {
        profileService.changePassword(user.id(), body);

        // New session id after a password change
        request.changeSessionId();

        return new MessageResponse("Your password has been changed.");
    }

    // The website calls this before showing checkout. 403 with code EMAIL_NOT_VERIFIED if not verified.
    @GetMapping("/checkout-eligibility")
    public Map<String, Boolean> checkoutEligibility(@AuthenticationPrincipal AuthUser user) {
        emailVerificationGuard.requireVerifiedEmail(user.id());
        return Map.of("canCheckout", true);
    }
}
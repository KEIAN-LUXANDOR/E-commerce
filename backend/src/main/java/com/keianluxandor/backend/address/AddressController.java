package com.keianluxandor.backend.address;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;

import jakarta.validation.Valid;

// The customer is always taken from the login session, never from the request
@RestController
@RequestMapping("/api/v1/account/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal AuthUser user) {
        return addressService.list(user.id());
    }

    @GetMapping("/states")
    public List<String> states() {
        return IndianStates.ALL;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal AuthUser user,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal AuthUser user,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(user.id(), id, request);
    }

    @PutMapping("/{id}/default")
    public AddressResponse makeDefault(@AuthenticationPrincipal AuthUser user,
                                       @PathVariable UUID id) {
        return addressService.makeDefault(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        addressService.delete(user.id(), id);
    }
}
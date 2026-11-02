package com.keianluxandor.backend.address;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.common.NotFoundException;

@Service
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;
    private final ShippingCountries shippingCountries;

    public AddressService(AddressRepository addressRepository, ShippingCountries shippingCountries) {
        this.addressRepository = addressRepository;
        this.shippingCountries = shippingCountries;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw badRequest("You can save up to " + MAX_ADDRESSES + " addresses. Please delete one first.");
        }
        Checked checked = check(request);

        // The first address is always the default
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.makeDefault());
        if (makeDefault) {
            addressRepository.clearDefault(userId);
        }

        Address address = new Address(userId);
        address.apply(request, checked.country(), checked.phone(), checked.state(), checked.pincode());
        address.setDefaultAddress(makeDefault);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = findOwned(userId, addressId);
        Checked checked = check(request);

        if (Boolean.TRUE.equals(request.makeDefault()) && !address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address = findOwned(userId, addressId);
            address.setDefaultAddress(true);
        }

        address.apply(request, checked.country(), checked.phone(), checked.state(), checked.pincode());
        return AddressResponse.from(addressRepository.saveAndFlush(address));
    }

    @Transactional
    public AddressResponse makeDefault(UUID userId, UUID addressId) {
        Address address = findOwned(userId, addressId);
        if (!address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address = findOwned(userId, addressId);
            address.setDefaultAddress(true);
            address = addressRepository.saveAndFlush(address);
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        addressRepository.flush();

        // Keep one default: the newest remaining address takes over
        if (wasDefault) {
            addressRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    // The checked and cleaned-up values of a request
    private record Checked(String country, String phone, String state, String pincode) {
    }

    // Applies the rules of the chosen country to phone, state and postal code
    private Checked check(AddressRequest request) {
        String country = Countries.normalizeCode(request.country())
                .orElseThrow(() -> badRequest("Choose a valid country."));

        if (!shippingCountries.isAllowed(country)) {
            throw badRequest("Sorry, we don't deliver to " + Countries.name(country) + " yet.");
        }

        try {
            String phone = Countries.normalizePhone(country, request.phone());
            String pincode = Countries.normalizePostalCode(country, request.pincode());
            String state = checkState(country, request.state());
            return new Checked(country, phone, state, pincode);
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        }
    }

    private static String checkState(String country, String state) {
        if ("IN".equals(country)) {
            // Indian addresses keep using the fixed list (needed later for GST)
            return IndianStates.normalize(state)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Choose a valid Indian state or union territory."));
        }
        if (state == null && Countries.stateRequired(country)) {
            throw new IllegalArgumentException("State / province is required for " + Countries.name(country) + ".");
        }
        return state;
    }

    private Address findOwned(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found."));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
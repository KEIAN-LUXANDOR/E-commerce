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

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can save up to " + MAX_ADDRESSES + " addresses. Please delete one first.");
        }
        String state = validState(request.state());

        // The first address is always the default
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.makeDefault());
        if (makeDefault) {
            addressRepository.clearDefault(userId);
        }

        Address address = new Address(userId);
        address.apply(request, state);
        address.setDefaultAddress(makeDefault);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = findOwned(userId, addressId);
        String state = validState(request.state());

        if (Boolean.TRUE.equals(request.makeDefault()) && !address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address = findOwned(userId, addressId);
            address.setDefaultAddress(true);
        }

        address.apply(request, state);
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

    private Address findOwned(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found."));
    }

    private String validState(String state) {
        return IndianStates.normalize(state)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a valid Indian state or union territory."));
    }
}
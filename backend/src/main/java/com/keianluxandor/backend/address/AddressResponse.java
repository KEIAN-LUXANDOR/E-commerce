package com.keianluxandor.backend.address;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String fullName,
        String phone,
        String line1,
        String line2,
        String landmark,
        String city,
        String state,
        String pincode,
        boolean defaultAddress
) {
    static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.isDefaultAddress());
    }
}
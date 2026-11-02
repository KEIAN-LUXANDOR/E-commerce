package com.keianluxandor.backend.address;

// One entry of the country dropdown, with hints for the address form:
// usesPostalCode=false -> hide the postal code field (e.g. UAE)
// postalCodeRequired / stateRequired -> mark the field as required
public record CountryResponse(
        String code,
        String name,
        boolean usesPostalCode,
        boolean postalCodeRequired,
        boolean stateRequired
) {
    static CountryResponse of(String code) {
        return new CountryResponse(
                code,
                Countries.name(code),
                Countries.usesPostalCode(code),
                Countries.postalCodeRequired(code),
                Countries.stateRequired(code));
    }
}
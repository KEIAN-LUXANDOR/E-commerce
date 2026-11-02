package com.keianluxandor.backend.address;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// The countries we deliver to, from the setting app.shipping.countries
// (2-letter codes separated by commas, e.g. "US,GB,CA,AE,AU,IN"). Empty = every country.
@Component
public class ShippingCountries {

    private final Set<String> allowed;

    public ShippingCountries(@Value("${app.shipping.countries:}") String setting) {
        this.allowed = Arrays.stream(setting.split(","))
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        // A typo in the setting should stop the backend at startup, not silently block a country
        allowed.stream()
                .filter(code -> !Countries.isValidCode(code))
                .findFirst()
                .ifPresent(code -> {
                    throw new IllegalStateException("app.shipping.countries contains an unknown country code: " + code);
                });
    }

    public boolean isAllowed(String countryCode) {
        return allowed.isEmpty() || allowed.contains(countryCode);
    }

    // For the country dropdown on the address form, sorted by name
    public List<CountryResponse> list() {
        List<String> codes = allowed.isEmpty() ? Countries.allCodes() : List.copyOf(allowed);
        return codes.stream()
                .map(CountryResponse::of)
                .sorted(Comparator.comparing(CountryResponse::name))
                .toList();
    }
}
package com.keianluxandor.backend.address;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// The 28 states and 8 union territories. The exact name matters later for GST.
public final class IndianStates {

    public static final List<String> ALL = List.of(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
            "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
            "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
            "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Andaman and Nicobar Islands", "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu", "Delhi",
            "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry");

    private static final Map<String, String> BY_LOWER_CASE = ALL.stream()
            .collect(Collectors.toMap(s -> s.toLowerCase(Locale.ROOT), Function.identity()));

    private IndianStates() {
    }

    // "maharashtra" -> "Maharashtra". Empty if not a valid state.
    public static Optional<String> normalize(String input) {
        if (input == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_LOWER_CASE.get(input.trim().toLowerCase(Locale.ROOT)));
    }
}
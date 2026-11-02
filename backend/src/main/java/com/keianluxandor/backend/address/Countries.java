package com.keianluxandor.backend.address;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// Country codes (ISO 3166-1 alpha-2: US, GB, AE, IN ...) and the address rules for each country.
// Problems are reported as IllegalArgumentException with a message that can be shown to the customer.
public final class Countries {

    private static final Set<String> ISO_CODES = Set.of(Locale.getISOCountries());

    // Countries where the postal code is required, with its format (after cleaning up)
    private static final Map<String, Pattern> POSTAL_FORMATS = Map.ofEntries(
            Map.entry("US", Pattern.compile("^\\d{5}(-\\d{4})?$")),                 // 94105 or 94105-1234
            Map.entry("CA", Pattern.compile("^[A-Z]\\d[A-Z] \\d[A-Z]\\d$")),        // K1A 0B1
            Map.entry("GB", Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]? \\d[A-Z]{2}$")), // SW1A 1AA
            Map.entry("IN", Pattern.compile("^[1-9]\\d{5}$")),                      // 411001
            Map.entry("AU", Pattern.compile("^\\d{4}$")),
            Map.entry("NZ", Pattern.compile("^\\d{4}$")),
            Map.entry("SG", Pattern.compile("^\\d{6}$")),
            Map.entry("DE", Pattern.compile("^\\d{5}$")),
            Map.entry("FR", Pattern.compile("^\\d{5}$")),
            Map.entry("IT", Pattern.compile("^\\d{5}$")),
            Map.entry("ES", Pattern.compile("^\\d{5}$")),
            Map.entry("CH", Pattern.compile("^\\d{4}$")),
            Map.entry("NL", Pattern.compile("^\\d{4} [A-Z]{2}$")),                  // 1012 AB
            Map.entry("JP", Pattern.compile("^\\d{3}-\\d{4}$")));                   // 100-0001

    // Countries that don't use postal codes: anything sent is ignored
    private static final Set<String> NO_POSTAL_CODE = Set.of("AE", "HK", "QA", "MO");

    // Countries where the state / province must be filled in
    private static final Set<String> STATE_REQUIRED = Set.of("IN", "US", "CA", "AU");

    // Any other country: optional; letters, digits, space and dash; 3 to 10 characters
    private static final Pattern GENERIC_POSTAL = Pattern.compile("^[A-Z0-9][A-Z0-9 -]{1,8}[A-Z0-9]$");

    // International phone format after cleaning: + then 7 to 15 digits
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private Countries() {
    }

    // " us " -> "US". "UK" is accepted as GB. Empty if it's not a real country code.
    public static Optional<String> normalizeCode(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String code = input.trim().toUpperCase(Locale.ROOT);
        if (code.equals("UK")) {
            code = "GB";
        }
        return ISO_CODES.contains(code) ? Optional.of(code) : Optional.empty();
    }

    public static boolean isValidCode(String code) {
        return code != null && ISO_CODES.contains(code);
    }

    // "US" -> "United States"
    public static String name(String code) {
        if (code == null) {
            return null;
        }
        return Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
    }

    public static List<String> allCodes() {
        return ISO_CODES.stream().sorted().toList();
    }

    public static boolean usesPostalCode(String code) {
        return !NO_POSTAL_CODE.contains(code);
    }

    public static boolean postalCodeRequired(String code) {
        return POSTAL_FORMATS.containsKey(code);
    }

    public static boolean stateRequired(String code) {
        return STATE_REQUIRED.contains(code);
    }

    // Cleans up and checks the postal code. Returns null when the country has none.
    // "sw1a1aa" -> "SW1A 1AA",  "k1a0b1" -> "K1A 0B1",  "1000001" (JP) -> "100-0001"
    public static String normalizePostalCode(String country, String raw) {
        if (NO_POSTAL_CODE.contains(country)) {
            return null;
        }
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        Pattern format = POSTAL_FORMATS.get(country);

        if (value.isEmpty()) {
            if (format != null) {
                throw new IllegalArgumentException("Postal code is required for " + name(country) + ".");
            }
            return null;
        }

        String compact = value.replace(" ", "");
        value = switch (country) {
            case "GB" -> compact.length() > 3
                    ? compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3)
                    : compact;
            case "CA" -> compact.length() == 6 ? compact.substring(0, 3) + " " + compact.substring(3) : compact;
            case "NL" -> compact.length() == 6 ? compact.substring(0, 4) + " " + compact.substring(4) : compact;
            case "JP" -> {
                String digits = compact.replace("-", "");
                yield digits.length() == 7 ? digits.substring(0, 3) + "-" + digits.substring(3) : compact;
            }
            default -> value;
        };

        Pattern check = format != null ? format : GENERIC_POSTAL;
        if (!check.matcher(value).matches()) {
            throw new IllegalArgumentException("Enter a valid postal code for " + name(country) + ".");
        }
        return value;
    }

    // "+1 (415) 555-0100" -> "+14155550100". "0044..." -> "+44...".
    // For India, a 10-digit mobile number without +91 is also accepted.
    public static String normalizePhone(String country, String raw) {
        String value = raw == null ? "" : raw.replaceAll("[\\s().-]", "");
        if (value.startsWith("00")) {
            value = "+" + value.substring(2);
        }
        if (!value.startsWith("+") && "IN".equals(country) && value.matches("^[6-9]\\d{9}$")) {
            value = "+91" + value;
        }
        if (!PHONE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Enter the phone number with its country code, for example +1 415 555 0100.");
        }
        if (value.startsWith("+91") && !value.matches("^\\+91[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number after +91.");
        }
        return value;
    }
}
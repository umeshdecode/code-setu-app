package com.cargosetu.marketplace.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable reference numbers such as {@code LD-20261121-7KQ2XM}.
 * Replaces the old {@code count() + 1} scheme, which produced duplicates under
 * concurrent inserts or after rows were deleted (and then hit the unique constraint).
 */
public final class MarketplaceNumbers {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MarketplaceNumbers() {}

    public static String next(String prefix) {
        StringBuilder sb = new StringBuilder(prefix).append('-')
                .append(LocalDate.now().format(DAY)).append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

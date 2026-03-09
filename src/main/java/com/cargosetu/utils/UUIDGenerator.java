package com.cargosetu.utils;

import java.security.SecureRandom;

public class UUIDGenerator {
    private static final String PREFIX = "SCSETU";
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 9;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    public static String generate(String prefix,String suffix) {
        if(prefix == null || prefix.isBlank()) prefix = PREFIX;
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.append(suffix).toString();
    }
}

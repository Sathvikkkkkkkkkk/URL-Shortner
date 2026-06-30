package com.urlshortener.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

public class Base62Encoder {

    private static final String CHARSET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = CHARSET.length();

    /**
     * Encodes a string (URL + timestamp + optional salt) to a 6-character Base62 string.
     *
     * @param originalUrl The long URL.
     * @param timestamp   The timestamp when shortening is requested.
     * @param salt        The collision resolution salt.
     * @return A 6-character Base62 short code.
     */
    public static String encode(String originalUrl, long timestamp, int salt) {
        try {
            // Concatenate inputs to form the hash message
            String input = originalUrl + timestamp + (salt > 0 ? "_" + salt : "");
            
            // Generate MD5 digest
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array into a positive BigInteger
            BigInteger value = new BigInteger(1, hashBytes);
            
            // Encode value to Base62
            StringBuilder sb = new StringBuilder();
            BigInteger baseVal = BigInteger.valueOf(BASE);
            while (value.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divRem = value.divideAndRemainder(baseVal);
                sb.append(CHARSET.charAt(divRem[1].intValue()));
                value = divRem[0];
            }
            
            String encoded = sb.toString();
            
            // Handle edge cases of short length by padding
            if (encoded.length() < 6) {
                StringBuilder padder = new StringBuilder(encoded);
                while (padder.length() < 6) {
                    padder.append(CHARSET.charAt(0));
                }
                encoded = padder.toString();
            }
            
            // Take the first 6 characters
            return encoded.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("System missing MD5 message digest provider", e);
        }
    }
}

package org.hestiastore.index.benchmark;

import java.util.Random;

/**
 * Class provides random strings and wraps long to 10 zeros.
 */
public class DataProvider {

    private final static Random RANDOM = new Random();
    private final static int DEFAULT_RANDOM_STRING_LENGTH = 20;
    private final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    String generateRandomString() {
        return generateRandomString(DEFAULT_RANDOM_STRING_LENGTH);
    }

    String generateRandomString(final int lenght) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lenght; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

    /*
     * Wrap long to 10 zeros, results should look like 0000000001, 0000000002,
     * ...
     */
    public String wrap(final long l) {
        String out = String.valueOf(l);
        while (out.length() < 10) {
            out = "0" + out;
        }
        return out;
    }

}

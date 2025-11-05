package org.hestiastore.index.benchmark.load;

import java.security.MessageDigest;

public class HashDataProvider {

    private final static String HEXES = "0123456789ABCDEF";

    public String makeHash(final long i) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final String str = String.valueOf(i);
            md.update(str.getBytes("ISO-8859-1"));
            byte[] digest = md.digest();
            return getHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

}

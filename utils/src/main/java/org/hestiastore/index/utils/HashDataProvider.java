package org.hestiastore.index.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Generates deterministic MD5 hashes for numeric inputs. Used by benchmarks to
 * create reproducible key sequences.
 */
public class HashDataProvider {

    private static final String HEXES = "0123456789ABCDEF";
    private final MessageDigest md;

    public HashDataProvider() {
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm not available", ex);
        }
    }

    public String makeHash(final long value) {
        synchronized (md) {
            md.reset();
            md.update(Long.toString(value).getBytes(StandardCharsets.ISO_8859_1));
            return toHex(md.digest());
        }
    }

    private String toHex(final byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(raw.length * 2);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}

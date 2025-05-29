package org.hestiastore.index.benchmark;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class RandomTest {
	
	@Test
	void test_random() throws Exception {
		
		final Random random = new Random(7835);
		
		    
		for (long i=0;i<1000;i++) {
			final long rnd = random.nextLong();
			final String hash = makeHash(rnd);
			System.out.println(hash);
		}
			
	}
	
	private String makeHash(final long i) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("MD5");
		final String str = String.valueOf(i);
		 md.update(str.getBytes("ISO-8859-1"));		 
		 byte[] digest = md.digest();
		return getHex(digest);
	}

	
	static final String HEXES = "0123456789ABCDEF";
	public static String getHex( byte [] raw ) {
	    if ( raw == null ) {
	        return null;
	    }
	    final StringBuilder hex = new StringBuilder( 2 * raw.length );
	    for ( final byte b : raw ) {
	        hex.append(HEXES.charAt((b & 0xF0) >> 4))
	            .append(HEXES.charAt((b & 0x0F)));
	    }
	    return hex.toString();
	}
	

}

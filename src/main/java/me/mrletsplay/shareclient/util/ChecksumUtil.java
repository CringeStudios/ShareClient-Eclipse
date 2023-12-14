package me.mrletsplay.shareclient.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtil {

	private static final MessageDigest SHA_256;

	static {
		try {
			SHA_256 = MessageDigest.getInstance("SHA-256");
		}catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] generateSHA256(byte[] bytes) {
		return SHA_256.digest(bytes);
	}

}

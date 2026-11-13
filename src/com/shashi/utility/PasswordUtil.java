package com.shashi.utility;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
	private static final String PREFIX = "pbkdf2_sha256";
	private static final int ITERATIONS = 210000;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BITS = 256;
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordUtil() {
	}

	public static boolean isValidNewPassword(String password) {
		return password != null && password.length() >= 12 && password.length() <= 128;
	}

	public static String hash(String password) throws GeneralSecurityException {
		if (!isValidNewPassword(password)) {
			throw new IllegalArgumentException("Password must be between 12 and 128 characters.");
		}

		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = derive(password, salt, ITERATIONS);
		return PREFIX + "$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
				+ Base64.getEncoder().encodeToString(hash);
	}

	public static boolean verify(String password, String storedPassword) throws GeneralSecurityException {
		if (password == null || storedPassword == null) {
			return false;
		}

		if (!storedPassword.startsWith(PREFIX + "$")) {
			return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
					storedPassword.getBytes(StandardCharsets.UTF_8));
		}

		String[] parts = storedPassword.split("\\$", -1);
		if (parts.length != 4 || !PREFIX.equals(parts[0])) {
			return false;
		}

		try {
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
			if (iterations < 100000 || iterations > 1000000 || salt.length != SALT_BYTES
					|| expectedHash.length != HASH_BITS / 8) {
				return false;
			}
			return MessageDigest.isEqual(expectedHash, derive(password, salt, iterations));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static boolean isHashed(String storedPassword) {
		return storedPassword != null && storedPassword.startsWith(PREFIX + "$");
	}

	private static byte[] derive(String password, byte[] salt, int iterations) throws GeneralSecurityException {
		char[] passwordChars = password.toCharArray();
		PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, HASH_BITS);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} finally {
			spec.clearPassword();
			java.util.Arrays.fill(passwordChars, '\0');
		}
	}
}

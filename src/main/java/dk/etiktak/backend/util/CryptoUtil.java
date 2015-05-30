package dk.etiktak.backend.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class CryptoUtil {

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    public static boolean validateHash(String plainText, String hashed) {
        return BCrypt.checkpw(plainText, hashed);
    }

    public static String generateSmsChallenge() throws NoSuchAlgorithmException {
        final int SMS_CHALLENGE_DIGITS = 5;

        int minValue = (int)Math.pow(10, SMS_CHALLENGE_DIGITS - 1);
        int maxValue = (minValue * 10) - 1;

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(random.generateSeed(20));
        return (random.nextInt(maxValue - minValue) + minValue) + "";
    }

    public static String generateSmsHandle() throws NoSuchAlgorithmException {
        final int SMS_HANDLE_BYTES = 16;
        final byte[] randomBytes = new byte[SMS_HANDLE_BYTES];

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(random.generateSeed(20));
        random.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}

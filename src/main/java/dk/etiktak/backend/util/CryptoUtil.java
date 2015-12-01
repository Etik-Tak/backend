// Copyright (c) 2015, Daniel Andersen (daniel@trollsahead.dk)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. The name of the author may not be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package dk.etiktak.backend.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class CryptoUtil {

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String hash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(text.getBytes("UTF-8"));

        return ArrayUtil.convertByteArrayToHexString(hashedBytes);
    }

    public static String encryptPassword(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    public static boolean validatePassword(String plainText, String hashed) {
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

    public static String hashOfHashes(String value1, String value2) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return hash(hash(value1) + hash(value2));
    }
}

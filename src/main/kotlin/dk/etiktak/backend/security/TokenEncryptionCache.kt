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

package dk.etiktak.backend.security

import com.fasterxml.jackson.databind.ObjectMapper
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.encrypt.TextEncryptor

open class TokenEncryptionCache {
    private val logger = LoggerFactory.getLogger(TokenEncryptionCache::class.java)

    companion object {
        val sharedInstance = TokenEncryptionCache()
        val encryptorCount = 2
    }

    private var textEncryptors = arrayListOf<TextEncryptor>()

    constructor() {
        for (i in 1..encryptorCount) {
            generateNewEncryptor()
        }
    }

    /**
     * Encrypts the given token cache entry with first text encryptor.
     *
     * @param tokenCacheEntry  Token cache entry
     * @return                 Encrypted token
     */
    fun encryptToken(tokenCacheEntry: TokenCacheEntry): String {
        val jsonString = ObjectMapper().writeValueAsString(tokenCacheEntry)
        return textEncryptors[0].encrypt(jsonString)
    }

    /**
     * Decrypts the given token with the text encryptor at the given index.
     *
     * @param token      Token
     * @return           Decrypted token cache entry
     */
    fun decryptToken(token: String): TokenCacheEntry {
        for (i in 0..(TokenEncryptionCache.encryptorCount - 1)) {
            try {
                val jsonString = textEncryptors[i].decrypt(token)
                return ObjectMapper().readValue(jsonString, TokenCacheEntry::class.java)
            } catch (e: Exception) {
                logger.debug("Could not decrypt token", e)
            }
        }
        throw BadCredentialsException("Invalid token")
    }

    /**
     * Generates a new encryptor. Rolls the encryptor cache.
     */
    fun generateNewEncryptor() {
        logger.info("Creating new token encryptor")

        // Create new encryptor
        textEncryptors.add(0, CryptoUtil().createTextEncryptor())

        // Remove last
        if (textEncryptors.size > encryptorCount) {
            textEncryptors.remove(textEncryptors.last())
        }
    }
}
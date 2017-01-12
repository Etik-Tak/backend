// Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)
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

import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.user.ClientRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import java.util.*

@Service
open class TokenService @Autowired constructor(
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(TokenService::class.java)

    companion object {
        val timeout: Long = 2 * 60 * 60 * 1000
    }

    @Scheduled(fixedRate = 2 * (1 * 60 * 60 * 1000))
    fun generateNewEncryptor() {
        TokenEncryptionCache.sharedInstance.generateNewEncryptor()
    }

    /**
     * Generates a new token from the given client.
     *
     * @param client   Client
     * @return         Token
     */
    fun generateNewToken(client: Client): String {
        return TokenEncryptionCache.sharedInstance.encryptToken(TokenCacheEntry(client.uuid, Date().time))
    }

    /**
     * Finds the client from a given token, or null, if the token is invalid or outdated.
     *
     * @param token    Token
     * @return         Client
     */
    fun getClientFromToken(token: String): Client? {
        val now = Date().time

        val tokenCacheEntry = TokenEncryptionCache.sharedInstance.decryptToken(token)

        if (now - tokenCacheEntry.creationTime!! <= timeout) {
            return clientRepository.findByUuid(tokenCacheEntry.clientUuid!!) ?: throw BadCredentialsException("Invalid token")
        } else {
            throw BadCredentialsException("Token has timed out")
        }
    }
}
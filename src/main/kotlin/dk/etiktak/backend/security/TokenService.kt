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

import dk.etiktak.backend.util.CryptoUtil
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.CacheManagerBuilder
import org.ehcache.config.CacheConfigurationBuilder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
open class TokenService {

    private val logger = LoggerFactory.getLogger(TokenService::class.java)

    private val cacheManager: CacheManager

    private var cache: Cache<String, Authentication>

    constructor() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("restApiAuthTokenCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder<String, Authentication>().buildConfig(String::class.java, Authentication::class.java))
                .build(true)

        cache = cacheManager.getCache("restApiAuthTokenCache", String::class.java, Authentication::class.java)
    }

    @Scheduled(fixedRate = 30*60*1000)
    fun evictExpiredTokens() {
        logger.info("Evicting expired tokens")
        //cache.evictExpiredElements()
    }

    fun generateNewToken(): String {
        return CryptoUtil().uuid()
    }

    fun store(token: String, authentication: Authentication) {
        cache.put(token, authentication)
    }

    operator fun contains(token: String): Boolean {
        return cache.containsKey(token)
    }

    fun retrieve(token: String): Authentication {
        return cache.get(token)
    }
}
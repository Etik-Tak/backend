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
import dk.etiktak.backend.config.WebSecurityConfig
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.MDC
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.util.StringUtils
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.util.UrlPathHelper
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse

open class AuthenticationFilter constructor(
        private val authenticationManager: AuthenticationManager): GenericFilterBean() {

    val CLIENT_UUID_SESSION_KEY = "clientUuid"

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

        var httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val username = httpRequest.getHeader("X-Auth-Username")
        val password = httpRequest.getHeader("X-Auth-Password")
        val token = httpRequest.getHeader("X-Auth-Token")
        val deviceId = httpRequest.getHeader("X-Auth-DeviceId")

        val resourcePath = UrlPathHelper().getPathWithinApplication(httpRequest)

        try {
            if (isPostToAuthenticate(httpRequest, resourcePath)) {
                logger.info("Trying to authenticate user $username by X-Auth-Username method")
                processUsernamePasswordAuthentication(httpResponse, username, password)
                return
            }

            if (!StringUtils.isEmpty(deviceId)) {
                logger.info("Trying to authenticate user by X-Auth-DeviceId method. Device ID: $deviceId")
                processDeviceIdAuthentication(deviceId!!)
            }

            if (!StringUtils.isEmpty(token)) {
                logger.info("Trying to authenticate user by X-Auth-Token method. Token: $token")
                processTokenAuthentication(token!!)
            }

            logger.info("AuthenticationFilter is passing request down the filter chain")

            // Clear anonymous user
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication is AnonymousAuthenticationToken) {
                logger.info("Clearing anonymous user authentication")
                SecurityContextHolder.getContext().authentication = null
            }

            addSessionContextToLogging()
            chain.doFilter(httpRequest, response)

        } catch (internalAuthenticationServiceException: InternalAuthenticationServiceException) {
            SecurityContextHolder.clearContext()
            logger.error("Internal authentication service exception", internalAuthenticationServiceException)
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        } catch (authenticationException: AuthenticationException) {
            SecurityContextHolder.clearContext()
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, authenticationException.message)
        } finally {
            MDC.remove(CLIENT_UUID_SESSION_KEY)
        }
    }

    private fun processUsernamePasswordAuthentication(httpResponse: HttpServletResponse, username: String, password: String) {
        val resultOfAuthentication = tryToAuthenticateWithUsernameAndPassword(username, password)
        SecurityContextHolder.getContext().authentication = resultOfAuthentication

        val tokenResponse = TokenResponse(resultOfAuthentication.details.toString())
        val tokenJsonResponse = ObjectMapper().writeValueAsString(tokenResponse)

        httpResponse.status = HttpServletResponse.SC_OK
        httpResponse.addHeader("Content-Type", "application/json")
        httpResponse.writer.print(tokenJsonResponse)
    }

    private fun tryToAuthenticateWithUsernameAndPassword(username: String, password: String): Authentication {
        val requestAuthentication = UsernamePasswordAuthenticationToken(username, password)
        return tryToAuthenticate(requestAuthentication)
    }

    private fun processTokenAuthentication(token: String) {
        val resultOfAuthentication = tryToAuthenticateWithToken(token)
        SecurityContextHolder.getContext().authentication = resultOfAuthentication
    }

    private fun tryToAuthenticateWithToken(token: String): Authentication {
        val requestAuthentication = PreAuthenticatedAuthenticationToken(token, null)
        return tryToAuthenticate(requestAuthentication)
    }

    private fun processDeviceIdAuthentication(deviceId: String) {
        val resultOfAuthentication = tryToAuthenticateWithDeviceId(deviceId)
        SecurityContextHolder.getContext().authentication = resultOfAuthentication
    }

    private fun tryToAuthenticateWithDeviceId(deviceId: String): Authentication {
        val requestAuthentication = DeviceAuthenticationToken(deviceId)
        return tryToAuthenticate(requestAuthentication)
    }

    private fun tryToAuthenticate(requestAuthentication: Authentication): Authentication {
        val responseAuthentication = authenticationManager.authenticate(requestAuthentication)
        if (responseAuthentication == null || !responseAuthentication.isAuthenticated) {
            throw InternalAuthenticationServiceException("Unable to authenticate user for provided credentials: $responseAuthentication")
        }
        logger.info("User successfully authenticated")
        return responseAuthentication
    }

    private fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.principal != null
    }

    private fun addSessionContextToLogging() {
        var tokenValue = "EMPTY"
        if (isAuthenticated()) {
            val authentication = SecurityContextHolder.getContext().authentication
            val client = authentication.principal as Client
            tokenValue = client.uuid
        }
        MDC.put(CLIENT_UUID_SESSION_KEY, tokenValue)
    }

    private fun isPostToAuthenticate(httpRequest: HttpServletRequest, resourcePath: String): Boolean {
        return WebSecurityConfig.authenticationEndpoint.equals(resourcePath) && httpRequest.method == "POST"
    }
}
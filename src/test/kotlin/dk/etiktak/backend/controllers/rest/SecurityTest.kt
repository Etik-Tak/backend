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

package dk.etiktak.backend.controllers.rest

import dk.etiktak.backend.Application
import dk.etiktak.backend.controller.rest.WebserviceResult
import dk.etiktak.backend.security.TokenEncryptionCache
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
open class SecurityTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
    }

    /**
     * Test that we can create a company by authenticating with device ID.
     */
    @Test
    fun authenticateWithDeviceId() {
        mockMvc().perform(
                post(serviceEndpoint("company/create/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("name", "Coca Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that we can create a company by authenticating with username and password.
     */
    @Test
    fun authenticateWithUsernameAndPassword() {

        // Create client with username and password
        createAndSaveClient(username = "test", password = "Test1234")

        // Authenticate to get token
        val token = postAndExtract(
                serviceEndpoint("authenticate"),
                hashMapOf(
                        "X-Auth-Username" to "test",
                        "X-Auth-Password" to "Test1234"),
                hashMapOf(),
                "$.token")

        // Create company with provided token
        mockMvc().perform(
                post(serviceEndpoint("company/create/"))
                        .header("X-Auth-Token", token)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that we cannot create a company with expired token.
     */
    @Test
    fun cannotCreateCompanyWithExpiredToken() {

        // Create client with username and password
        createAndSaveClient(username = "test", password = "Test1234")

        // Authenticate to get token
        val token = postAndExtract(
                serviceEndpoint("authenticate"),
                hashMapOf(
                        "X-Auth-Username" to "test",
                        "X-Auth-Password" to "Test1234"),
                hashMapOf(),
                "$.token")

        // Expire token by rolling keys
        for (i in 1..TokenEncryptionCache.encryptorCount) {
            tokenService!!.generateNewEncryptor()
        }

        // Create company with provided token
        mockMvc().perform(
                post(serviceEndpoint("company/create/"))
                        .header("X-Auth-Token", token)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isUnauthorized)
    }
}

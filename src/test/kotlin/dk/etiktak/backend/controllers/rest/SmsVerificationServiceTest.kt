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

package dk.etiktak.backend.controllers.rest

import dk.etiktak.backend.Application
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.model.user.SmsVerification
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import dk.etiktak.backend.util.CryptoUtil
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.web.util.NestedServletException
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class SmsVerificationServiceTest : BaseRestTest() {

    @Autowired
    private val clientRepository: ClientRepository? = null

    @Autowired
    private val mobileNumberRepository: MobileNumberRepository? = null

    @Autowired
    private val smsVerificationRepository: SmsVerificationRepository? = null

    @get:Rule
    public val exception = ExpectedException.none()

    private var client1: Client = Client()
    private var client2: Client = Client()
    private var smsChallenge: String = ""

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "verification/" + postfix
    }

    @Before
    @Throws(Exception::class)
    override fun setup() {
        super.setup()

        clientRepository!!.deleteAll()
        mobileNumberRepository!!.deleteAll()
        smsVerificationRepository!!.deleteAll()

        client1 = createAndSaveClient()
        client2 = createAndSaveClient()
    }

    @After
    fun tearDown() {
        clientRepository!!.deleteAll()
        mobileNumberRepository!!.deleteAll()
        smsVerificationRepository!!.deleteAll()
    }

    /**
     * Test that we can request a SMS verification.
     */
    @Test
    @Throws(Exception::class)
    fun requestSmsVerification() {

        // Request SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()))
    }

    /**
     * Test that we cannot request a SMS verification with empty password.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestSmsVerificationWithEmptyPasssword() {

        // Request SMS verification with empty password
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678"))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we cannot request a SMS verification with empty client uuid.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestSmsVerificationWithEmptyClientUuid() {

        // Request SMS verification with empty client uuid
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we cannot request a SMS verification with empty mobile number.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestSmsVerificationWithEmptyMobileNumber() {

        // Request SMS verification with empty mobile number
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("password", "test1234"))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we cannot request a SMS verification with no parameters.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestSmsVerificationWithNoParameters() {

        // Request SMS verification with no parameters
        mockMvc().perform(
                post(serviceEndpoint("request/")))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we can request two SMS verifications to same mobile number for same client.
     */
    @Test
    @Throws(Exception::class)
    fun requestMultipleSmsVerificationsToSameMobileNumber() {

        // Request first SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()))

        // Request second SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()))
    }

    /**
     * Test that we cannot request two SMS verifications to same mobile number for different clients.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestMultipleSmsVerificationsToSameMobileNumberWithDifferentClients() {

        // Request first SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()))

        // Request second SMS verification with another client uuid
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client2.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "anotherPassword"))
    }

    /**
     * Test that we can verify a SMS verification.
     */
    @Test
    @Throws(Exception::class)
    fun verifySmsVerification() {
        val smsVerification = requestAndModifySmsVerification()

        // Verify challenge
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientUuid", `is`(client1.uuid)))
    }

    /**
     * Test that we cannot verify a SMS verification with wrong SMS challenge.
     */
    @Test
    @Throws(Exception::class)
    fun cannotVerifySmsVerificationWithWrongSmsChallenge() {
        val smsVerification = requestAndModifySmsVerification()

        // Verify challenge
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge + "_wrong")
                        .param("clientChallenge", smsVerification.clientChallenge))
    }

    /**
     * Test that we cannot verify a SMS verification with wrong client challenge.
     */
    @Test
    @Throws(Exception::class)
    fun cannotVerifySmsVerificationWithWrongClientChallenge() {
        val smsVerification = requestAndModifySmsVerification()

        // Verify challenge
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge + "_wrong"))
    }

    /**
     * Test that we cannot verify a SMS verification with wrong mobile number.
     */
    @Test
    @Throws(Exception::class)
    fun cannotVerifySmsVerificationWithWrongMobileNumber() {
        val smsVerification = requestAndModifySmsVerification()

        // Verify challenge
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "wrong")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
    }

    /**
     * Test that we cannot verify a SMS verification with wrong password.
     */
    @Test
    @Throws(Exception::class)
    fun cannotVerifySmsVerificationWithWrongPassword() {
        val smsVerification = requestAndModifySmsVerification()

        // Verify challenge
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "wrong_password")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
    }

    /**
     * Test that we can request a recovery SMS verification.
     */
    @Test
    @Throws(Exception::class)
    fun requestRecoverySmsVerification() {
        requestAndVerifySmsVerification()

        // Request SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()))
    }

    /**
     * Test that we cannot request a recovery SMS verification with empty password.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestRecoverySmsVerificationWithEmptyPassword() {
        requestAndVerifySmsVerification()

        // Request SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678"))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we cannot request a recovery SMS verification with empty mobile number.
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestRecoverySmsVerificationWithEmptyMobileNumber() {
        requestAndVerifySmsVerification()

        // Request SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("password", "test1234"))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we cannot request a recovery SMS verification with no parameters
     */
    @Test
    @Throws(Exception::class)
    fun cannotRequestRecoverySmsVerificationWithNoParameters() {
        requestAndVerifySmsVerification()

        // Request SMS verification
        mockMvc().perform(
                post(serviceEndpoint("request/recovery/")))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we can verify a recovery SMS verification.
     */
    @Test
    @Throws(Exception::class)
    fun verifyRecoverySmsVerification() {
        requestAndVerifySmsVerification()
        val smsVerification = requestAndModifyRecoverySmsVerification()

        // Verify challenge
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientUuid", `is`(client1.uuid)))
    }

    /**
     * Test that we cannot verify a recovery SMS verification with empty password.
     */
    @Test
    @Throws(Exception::class)
    fun cannotVerifyRecoverySmsVerificationWithEmptyPassword() {
        requestAndVerifySmsVerification()
        val smsVerification = requestAndModifyRecoverySmsVerification()

        // Verify challenge
        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
                .andExpect(status().`is`(400))
    }


    @Throws(Exception::class)
    private fun requestAndVerifySmsVerification() {
        val smsVerification = requestAndModifySmsVerification()

        mockMvc().perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.clientChallenge))
    }

    @Throws(Exception::class)
    private fun requestAndModifySmsVerification(): SmsVerification {
        mockMvc().perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.uuid)
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))

        return modifySmsVerification()
    }

    @Throws(Exception::class)
    private fun requestAndModifyRecoverySmsVerification(): SmsVerification {
        mockMvc().perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))

        return modifySmsVerification()
    }

    @Throws(Exception::class)
    private fun modifySmsVerification(): SmsVerification {

        // Overwrite generated challenge to get raw, unhashed challenge in hand
        val smsVerification = smsVerificationRepository!!.findByMobileNumberHash(CryptoUtil().hash("12345678"))!!
        smsChallenge = CryptoUtil().generateSmsChallenge()
        smsVerification.smsChallengeHash = CryptoUtil().hash(smsChallenge)
        smsVerificationRepository.save(smsVerification)

        return smsVerification
    }

    private fun createAndSaveClient(): Client {
        val client = Client()
        client.uuid = CryptoUtil().uuid()
        client.verified = false
        clientRepository!!.save(client)
        return client
    }
}
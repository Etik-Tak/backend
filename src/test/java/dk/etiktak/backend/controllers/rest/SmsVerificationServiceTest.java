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

package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.Application;
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.SmsVerification;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
import dk.etiktak.backend.util.CryptoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.util.NestedServletException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class SmsVerificationServiceTest extends BaseRestTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Client client1;
    private Client client2;
    private String smsChallenge;

    public static String serviceEndpoint(String postfix) {
        return serviceEndpoint() + "verification/" + postfix;
    }

    @Before
    public void setup() throws Exception {
        super.setup();

        clientRepository.deleteAll();
        mobileNumberRepository.deleteAll();
        smsVerificationRepository.deleteAll();

        client1 = createAndSaveClient();
        client2 = createAndSaveClient();
    }

    @After
    public void tearDown() {
        clientRepository.deleteAll();
        mobileNumberRepository.deleteAll();
        smsVerificationRepository.deleteAll();
    }

    /**
     * Test that we can request a SMS verification.
     */
    @Test
    public void requestSmsVerification() throws Exception {

        // Request SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we cannot request a SMS verification with empty password.
     */
    @Test
    public void cannotRequestSmsVerificationWithEmptyPasssword() throws Exception {

        // Request SMS verification with empty password
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678"))
                .andExpect(status().is(400));
    }

    /**
     * Test that we cannot request a SMS verification with empty client uuid.
     */
    @Test
    public void cannotRequestSmsVerificationWithEmptyClientUuid() throws Exception {

        // Request SMS verification with empty client uuid
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().is(400));
    }

    /**
     * Test that we cannot request a SMS verification with empty mobile number.
     */
    @Test
    public void cannotRequestSmsVerificationWithEmptyMobileNumber() throws Exception {

        // Request SMS verification with empty mobile number
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("password", "test1234"))
                .andExpect(status().is(400));
    }

    /**
     * Test that we cannot request a SMS verification with no parameters.
     */
    @Test
    public void cannotRequestSmsVerificationWithNoParameters() throws Exception {

        // Request SMS verification with no parameters
        mockMvc.perform(
                post(serviceEndpoint("request/")))
                .andExpect(status().is(400));
    }

    /**
     * Test that we can request two SMS verifications to same mobile number for same client.
     */
    @Test
    public void requestMultipleSmsVerificationsToSameMobileNumber() throws Exception {

        // Request first SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

        // Request second SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we cannot request two SMS verifications to same mobile number for different clients.
     */
    @Test
    public void cannotRequestMultipleSmsVerificationsToSameMobileNumberWithDifferentClients() throws Exception {

        // Request first SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

        // Request second SMS verification with another client uuid
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client2.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "anotherPassword"));
    }

    /**
     * Test that we can verify a SMS verification.
     */
    @Test
    public void verifySmsVerification() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        // Verify challenge
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientUuid", is(client1.getUuid())));
    }

    /**
     * Test that we cannot verify a SMS verification with wrong SMS challenge.
     */
    @Test
    public void cannotVerifySmsVerificationWithWrongSmsChallenge() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        // Verify challenge
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge + "_wrong")
                        .param("clientChallenge", smsVerification.getClientChallenge()));
    }

    /**
     * Test that we cannot verify a SMS verification with wrong client challenge.
     */
    @Test
    public void cannotVerifySmsVerificationWithWrongClientChallenge() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        // Verify challenge
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge() + "_wrong"));
    }

    /**
     * Test that we cannot verify a SMS verification with wrong mobile number.
     */
    @Test
    public void cannotVerifySmsVerificationWithWrongMobileNumber() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        // Verify challenge
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "wrong")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()));
    }

    /**
     * Test that we cannot verify a SMS verification with wrong password.
     */
    @Test
    public void cannotVerifySmsVerificationWithWrongPassword() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        // Verify challenge
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "wrong_password")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()));
    }

    /**
     * Test that we can request a recovery SMS verification.
     */
    @Test
    public void requestRecoverySmsVerification() throws Exception {
        requestAndVerifySmsVerification();

        // Request SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we cannot request a recovery SMS verification with empty password.
     */
    @Test
    public void cannotRequestRecoverySmsVerificationWithEmptyPassword() throws Exception {
        requestAndVerifySmsVerification();

        // Request SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678"))
                .andExpect(status().is(400));
    }

    /**
     * Test that we cannot request a recovery SMS verification with empty mobile number.
     */
    @Test
    public void cannotRequestRecoverySmsVerificationWithEmptyMobileNumber() throws Exception {
        requestAndVerifySmsVerification();

        // Request SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("password", "test1234"))
                .andExpect(status().is(400));
    }

    /**
     * Test that we cannot request a recovery SMS verification with no parameters
     */
    @Test
    public void cannotRequestRecoverySmsVerificationWithNoParameters() throws Exception {
        requestAndVerifySmsVerification();

        // Request SMS verification
        mockMvc.perform(
                post(serviceEndpoint("request/recovery/")))
                .andExpect(status().is(400));
    }

    /**
     * Test that we can verify a recovery SMS verification.
     */
    @Test
    public void verifyRecoverySmsVerification() throws Exception {
        requestAndVerifySmsVerification();
        SmsVerification smsVerification = requestAndModifyRecoverySmsVerification();

        // Verify challenge
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientUuid", is(client1.getUuid())));
    }

    /**
     * Test that we cannot verify a recovery SMS verification with empty password.
     */
    @Test
    public void cannotVerifyRecoverySmsVerificationWithEmptyPassword() throws Exception {
        requestAndVerifySmsVerification();
        SmsVerification smsVerification = requestAndModifyRecoverySmsVerification();

        // Verify challenge
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()))
                .andExpect(status().is(400));
    }



    private void requestAndVerifySmsVerification() throws Exception {
        SmsVerification smsVerification = requestAndModifySmsVerification();

        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()));
    }

    private SmsVerification requestAndModifySmsVerification() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("request/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"));

        return modifySmsVerification();
    }

    private SmsVerification requestAndModifyRecoverySmsVerification() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("request/recovery/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"));

        return modifySmsVerification();
    }

    private SmsVerification modifySmsVerification() throws Exception {

        // Overwrite generated challenge to get raw, unhashed challenge in hand
        SmsVerification smsVerification = smsVerificationRepository.findByMobileNumberHash(new CryptoUtil().hash("12345678"));
        smsChallenge = new CryptoUtil().generateSmsChallenge();
        smsVerification.setSmsChallengeHash(new CryptoUtil().hash(smsChallenge));
        smsVerificationRepository.save(smsVerification);

        return smsVerification;
    }

    private Client createAndSaveClient() {
        Client client = new Client();
        client.setUuid(new CryptoUtil().uuid());
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }
}

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
import dk.etiktak.backend.service.client.SmsVerificationService;
import dk.etiktak.backend.util.CryptoUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.util.NestedServletException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    /**
     * Test that we can send a SMS verification.
     */
    @Test
    public void sendSmsVerification() throws Exception {

        // Send SMS verification
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we can send a SMS verification with empty password.
     */
    @Test
    public void sendSmsVerificationWithEmptyPasssword() throws Exception {

        // Send SMS verification with empty password
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we can send two SMS verifications to same mobile number for same client.
     */
    public void sendMultipleSmsVerificationsToSameMobileNumber() throws Exception {

        // Send first SMS verification
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

        // Send second SMS verification
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));
    }

    /**
     * Test that we cannot send two SMS verifications to same mobile number for different clients.
     */
    @Test(expected=NestedServletException.class)
    public void cannotSendMultipleSmsVerificationsToSameMobileNumberWithDifferentClients() throws Exception {

        // Send first SMS verification
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

        // Send second SMS verification with another client uuid
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client2.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "anotherPassword"));
    }

    /**
     * Test that we can verify a SMS verification.
     */
    @Test
    public void verifySmsVerification() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerification();

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
     * Test that we can verify a SMS verification with empty password.
     */
    @Test
    public void verifySmsVerificationWithEmptyPassword() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerificationWithEmptyPassword();

        // Verify challenge
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
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
    @Test(expected=NestedServletException.class)
    public void cannotVerifySmsVerificationWithWrongSmsChallenge() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerification();

        // Verify challenge
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
    @Test(expected=NestedServletException.class)
    public void cannotVerifySmsVerificationWithWrongClientChallenge() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerification();

        // Verify challenge
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
    @Test(expected=NestedServletException.class)
    public void cannotVerifySmsVerificationWithWrongMobileNumber() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerification();

        // Verify challenge
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
    @Test(expected=NestedServletException.class)
    public void cannotVerifySmsVerificationWithWrongPassword() throws Exception {
        SmsVerification smsVerification = sendAndModifySmsVerification();

        // Verify challenge
        mockMvc.perform(
                post(serviceEndpoint("verify/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "wrong_password")
                        .param("smsChallenge", smsChallenge)
                        .param("clientChallenge", smsVerification.getClientChallenge()));
    }



    private SmsVerification sendAndModifySmsVerification() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"));

        return modifySmsVerification();
    }

    private SmsVerification sendAndModifySmsVerificationWithEmptyPassword() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678"));

        return modifySmsVerification();
    }

    private SmsVerification modifySmsVerification() throws Exception {
        // Overwrite generated challenge to get raw, unhashed challenge in hand
        SmsVerification smsVerification = smsVerificationRepository.findByMobileNumberHash(CryptoUtil.hash("12345678"));
        smsChallenge = CryptoUtil.generateSmsChallenge();
        smsVerification.setSmsChallengeHash(CryptoUtil.hash(smsChallenge));
        smsVerificationRepository.save(smsVerification);

        return smsVerification;
    }

    private Client createAndSaveClient() {
        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }
}

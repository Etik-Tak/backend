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
        // Send SMS verification
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"));

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

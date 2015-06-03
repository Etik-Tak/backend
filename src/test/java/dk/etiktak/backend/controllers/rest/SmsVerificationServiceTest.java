package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.Application;
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
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
    @Test(expected=NestedServletException.class)
    public void cannotSendMultipleSmsVerificationsToSameMobileNumber1() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

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
    public void cannotSendMultipleSmsVerificationsToSameMobileNumber2() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client1.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientChallenge", notNullValue()));

        mockMvc.perform(
                post(serviceEndpoint("send/"))
                        .param("clientUuid", client2.getUuid())
                        .param("mobileNumber", "12345678")
                        .param("password", "anotherPassword"));
    }

    private Client createAndSaveClient() {
        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }
}

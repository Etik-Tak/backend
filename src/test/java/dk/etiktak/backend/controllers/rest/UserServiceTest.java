package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.Application;
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
import dk.etiktak.backend.repository.user.UserRepository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class UserServiceTest extends BaseRestTest {
    private MediaType jsonContentType = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream().filter(
                hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    protected String serviceEndpoint(String postfix) {
        return serviceEndpoint() + "user/" + postfix;
    }

    @Before
    public void setup() throws Exception {
        mockMvc = webAppContextSetup(webApplicationContext).build();

        userRepository.deleteAll();
        clientRepository.deleteAll();
        mobileNumberRepository.deleteAll();
        smsVerificationRepository.deleteAll();

        //this.account = accountRepository.save(new Account(userName, "password"));
    }

    /**
     * Test that we can create a client.
     */
    @Test
    public void createClient() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("client/create/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)));
    }

    /**
     * Test that we cannot create two clients with same mobile number.
     */
    @Test(expected=NestedServletException.class)
    public void cannotCreateClientsWithSameMobileNumber() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("client/create/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)));

        mockMvc.perform(
                post(serviceEndpoint("client/create/"))
                        .param("mobileNumber", "12345678")
                        .param("password", "test1234"))
                .andDo(print());
        ;
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}

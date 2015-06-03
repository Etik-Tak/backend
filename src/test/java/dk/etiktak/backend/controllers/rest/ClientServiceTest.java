package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.Application;
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.repository.user.ClientRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class ClientServiceTest extends BaseRestTest {

    @Autowired
    private ClientRepository clientRepository;

    public static String serviceEndpoint(String postfix) {
        return serviceEndpoint() + "client/" + postfix;
    }

    @Before
    public void setup() throws Exception {
        super.setup();

        clientRepository.deleteAll();
    }

    /**
     * Test that we can create a client.
     */
    @Test
    public void createClient() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint("create/")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.clientUuid", notNullValue()));
    }
}

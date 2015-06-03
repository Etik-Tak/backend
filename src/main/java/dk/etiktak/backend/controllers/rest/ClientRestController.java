package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.controllers.rest.json.ClientJsonObject;
import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.service.client.ClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service/client")
public class ClientRestController extends BaseRestController {

    @Autowired
    private ClientService clientService;

    @RequestMapping(value = "/create/", method = RequestMethod.POST)
    public ClientJsonObject create() throws Exception {
        Client client = clientService.createClient();
        return new ClientJsonObject(client);
    }
}

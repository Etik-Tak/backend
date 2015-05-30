package dk.etiktak.backend.controllers;

import dk.etiktak.backend.model.Client;
import dk.etiktak.backend.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private ClientService clientService;

    @RequestMapping("/test/")
    @ResponseBody
    List<Client> test() {
        return clientService.findClients();
    }
}

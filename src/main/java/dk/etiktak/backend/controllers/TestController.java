package dk.etiktak.backend.controllers;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @RequestMapping("/test/")
    List<Client> test() {
        return userService.findClients();
    }
}

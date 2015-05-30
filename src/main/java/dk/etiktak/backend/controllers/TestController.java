package dk.etiktak.backend.controllers;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.MobileNumber;
import dk.etiktak.backend.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @RequestMapping("/test/")
    MobileNumber test() throws Exception {
        return userService.createMobileNumber("12345678");
    }


}

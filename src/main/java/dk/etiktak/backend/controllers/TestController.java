package dk.etiktak.backend.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    @RequestMapping("/")
    @ResponseBody
    TestObject home() {
        return new TestObject();
    }

    private class TestObject {
        public String username = "user";
        public String password = "test";
    }
}

package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.controllers.rest.json.SmsVerificationJsonObject;
import dk.etiktak.backend.model.user.SmsVerification;
import dk.etiktak.backend.service.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service/user")
public class UserRestController extends BaseRestController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/client/create/", method = RequestMethod.POST)
    public BaseJsonObject createUser(@RequestParam String mobileNumber,
                                     @RequestParam String password) throws Exception {
        userService.createUser(mobileNumber, password);
        return ok();
    }

    @RequestMapping(value = "/verification/send/", method = RequestMethod.POST)
    public SmsVerificationJsonObject sendUserChallenge(@RequestParam String mobileNumber) throws Exception {
        SmsVerification smsVerification = userService.sendSmsChallenge(mobileNumber);
        return new SmsVerificationJsonObject(smsVerification);
    }

    @RequestMapping(value = "/verification/verify/", method = RequestMethod.POST)
    public BaseJsonObject verifyUser(@RequestParam String mobileNumber,
                                     @RequestParam String password,
                                     @RequestParam String smsChallenge,
                                     @RequestParam String clientChallenge) throws Exception {
        userService.verifySmsChallenge(mobileNumber, password, smsChallenge, clientChallenge);
        return ok();
    }
}

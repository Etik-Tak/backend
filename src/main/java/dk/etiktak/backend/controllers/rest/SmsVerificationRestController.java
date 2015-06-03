package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.controllers.rest.json.SmsVerificationJsonObject;
import dk.etiktak.backend.model.user.SmsVerification;
import dk.etiktak.backend.service.client.SmsVerificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service/verification")
public class SmsVerificationRestController extends BaseRestController {

    @Autowired
    private SmsVerificationService smsVerificationService;

    @RequestMapping(value = "/send/", method = RequestMethod.POST)
    public SmsVerificationJsonObject sendUserChallenge(
            @RequestParam String clientUuid,
            @RequestParam String mobileNumber,
            @RequestParam String password) throws Exception {
        SmsVerification smsVerification = smsVerificationService.sendSmsChallenge(clientUuid, mobileNumber, password);
        return new SmsVerificationJsonObject(smsVerification);
    }

    @RequestMapping(value = "/verify/", method = RequestMethod.POST)
    public BaseJsonObject verifyUser(@RequestParam String mobileNumber,
                                     @RequestParam String password,
                                     @RequestParam String smsChallenge,
                                     @RequestParam String clientChallenge) throws Exception {
        smsVerificationService.verifySmsChallenge(mobileNumber, password, smsChallenge, clientChallenge);
        return ok();
    }
}

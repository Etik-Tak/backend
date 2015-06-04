package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.controllers.rest.json.ClientJsonObject;
import dk.etiktak.backend.controllers.rest.json.SmsVerificationJsonObject;
import dk.etiktak.backend.model.user.Client;
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
    public SmsVerificationJsonObject sendSmsChallenge(
            @RequestParam String clientUuid,
            @RequestParam String mobileNumber,
            @RequestParam String password) throws Exception {
        SmsVerification smsVerification = smsVerificationService.sendSmsChallenge(clientUuid, mobileNumber, password);
        return new SmsVerificationJsonObject(smsVerification);
    }

    @RequestMapping(value = "/send/recovery/", method = RequestMethod.POST)
    public SmsVerificationJsonObject sendRecoverySmsChallenge(
            @RequestParam String mobileNumber,
            @RequestParam String password) throws Exception {
        SmsVerification smsVerification = smsVerificationService.sendRecoverySmsChallenge(mobileNumber, password);
        return new SmsVerificationJsonObject(smsVerification);
    }

    @RequestMapping(value = "/verify/", method = RequestMethod.POST)
    public ClientJsonObject verifySmsChallenge(@RequestParam String mobileNumber,
                                               @RequestParam String password,
                                               @RequestParam String smsChallenge,
                                               @RequestParam String clientChallenge) throws Exception {
        Client client = smsVerificationService.verifySmsChallenge(mobileNumber, password, smsChallenge, clientChallenge);
        return new ClientJsonObject(client);
    }
}

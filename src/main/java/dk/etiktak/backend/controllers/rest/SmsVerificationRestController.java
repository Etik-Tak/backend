// Copyright (c) 2015, Daniel Andersen (daniel@trollsahead.dk)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. The name of the author may not be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package dk.etiktak.backend.controllers.rest;

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

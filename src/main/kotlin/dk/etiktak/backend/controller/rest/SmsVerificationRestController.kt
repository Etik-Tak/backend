// Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)
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

/**
 * Rest controller responsible for handling client verification.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.security.CurrentlyLoggedClient
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.client.SmsVerificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/verification")
class SmsVerificationRestController @Autowired constructor(
        private val smsVerificationService: SmsVerificationService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/request/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun requestSmsChallenge(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam mobileNumber: String,
            @RequestParam(required = false) recoveryEnabled: Boolean? = false): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        val smsVerification = smsVerificationService.requestSmsChallenge(client, mobileNumber, recoveryEnabled ?: false)

        return okMap().add(smsVerification)
    }

    @RequestMapping(value = "/request/recovery/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun requestRecoverySmsChallenge(
            @RequestParam mobileNumber: String): HashMap<String, Any> {

        val smsVerification = smsVerificationService.requestRecoverySmsChallenge(mobileNumber)

        return okMap().add(smsVerification)
    }

    @RequestMapping(value = "/verify/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun verifySmsChallenge(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam mobileNumber: String,
            @RequestParam smsChallenge: String,
            @RequestParam clientChallenge: String): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        smsVerificationService.verifySmsChallenge(client, mobileNumber, smsChallenge, clientChallenge,
                modifyValues = {modifiedClient -> client = modifiedClient})

        return okMap().add(client)
    }
}
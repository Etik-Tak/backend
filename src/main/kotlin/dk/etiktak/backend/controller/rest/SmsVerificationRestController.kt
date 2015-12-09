package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controllers.rest.json.ClientJsonObject
import dk.etiktak.backend.controllers.rest.json.SmsVerificationJsonObject
import dk.etiktak.backend.service.client.SmsVerificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/service/verification")
class SmsVerificationRestController : BaseRestController() {

    @Autowired
    private val smsVerificationService: SmsVerificationService? = null

    @RequestMapping(value = "/request/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun requestSmsChallenge(
            @RequestParam clientUuid: String,
            @RequestParam mobileNumber: String,
            @RequestParam password: String): SmsVerificationJsonObject {
        val smsVerification = smsVerificationService!!.requestSmsChallenge(clientUuid, mobileNumber, password)
        return SmsVerificationJsonObject(smsVerification)
    }

    @RequestMapping(value = "/request/recovery/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun requestRecoverySmsChallenge(
            @RequestParam mobileNumber: String,
            @RequestParam password: String): SmsVerificationJsonObject {
        val smsVerification = smsVerificationService!!.requestRecoverySmsChallenge(mobileNumber, password)
        return SmsVerificationJsonObject(smsVerification)
    }

    @RequestMapping(value = "/verify/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun verifySmsChallenge(@RequestParam mobileNumber: String,
                           @RequestParam password: String,
                           @RequestParam smsChallenge: String,
                           @RequestParam clientChallenge: String): ClientJsonObject {
        val client = smsVerificationService!!.verifySmsChallenge(mobileNumber, password, smsChallenge, clientChallenge)
        return ClientJsonObject(client)
    }
}
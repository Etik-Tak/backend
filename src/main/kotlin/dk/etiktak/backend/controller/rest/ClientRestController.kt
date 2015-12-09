package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controllers.rest.json.ClientJsonObject
import dk.etiktak.backend.service.client.ClientService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/service/client")
class ClientRestController : BaseRestController() {

    @Autowired
    private val clientService: ClientService? = null

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    @Throws(Exception::class)
    fun create(): ClientJsonObject {
        val client = clientService!!.createClient()
        return ClientJsonObject(client)
    }
}

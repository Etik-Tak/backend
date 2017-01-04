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

/**
 * Rest controller responsible for handling client lifecycle.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.model.user.ClientDevice
import dk.etiktak.backend.security.CurrentlyLoggedClient
import dk.etiktak.backend.service.client.ClientService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin(origins = arrayOf("http://localhost:9000"), allowedHeaders = arrayOf("Origin", "X-Requested-With", "Content-Type", "Accept"))
@RestController
@RequestMapping("/service/client")
class ClientRestController @Autowired constructor(
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun create(
            @RequestParam(required = false) deviceType: ClientDevice.DeviceType?,
            @RequestParam(required = false) username: String?,
            @RequestParam(required = false) password: String?): HashMap<String, Any> {

        val deviceId = clientService.createClient(deviceType ?: ClientDevice.DeviceType.Unknown, username, password)

        return okMap().add("device", hashMapOf("id" to deviceId))
    }

    @RequestMapping(value = "/device/create/", method = arrayOf(RequestMethod.POST))
    fun createDevice(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam(required = false) deviceType: ClientDevice.DeviceType? = ClientDevice.DeviceType.Unknown): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        val deviceId = clientService.createDevice(client, deviceType)

        return okMap().add("device", hashMapOf("id" to deviceId))
    }
}

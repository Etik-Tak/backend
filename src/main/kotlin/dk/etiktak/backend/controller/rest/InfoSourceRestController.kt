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
 * Rest controller responsible for handling info sources.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.infosource.InfoSourceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/infosource")
class InfoSourceRestController @Autowired constructor(
        private val infoSourceService: InfoSourceService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createInfoSource(
            @RequestHeader clientUuid: String,
            @RequestParam domainList: List<String>,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")

        val infoSource = infoSourceService.createInfoSource(client, domainList, name)

        return okMap().add(infoSource)
    }

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.POST))
    fun getInfoSourceReference(
            @RequestParam(required = false) url: String?,
            @RequestParam(required = false) uuid: String?): HashMap<String, Any> {
        url?.let {
            val infoSource = infoSourceService.getInfoSourceByUrl(url) ?: return notFoundMap("Info source")
            return okMap().add(infoSource)
        }

        uuid?.let {
            val infoSource = infoSourceService.getInfoSourceByUuid(uuid) ?: return notFoundMap("Info source")
            return okMap().add(infoSource)
        }

        return notFoundMap("Info source")
    }

    @RequestMapping(value = "/edit/", method = arrayOf(RequestMethod.POST))
    fun editInfoSource(
            @RequestHeader clientUuid: String,
            @RequestParam infoSourceUuid: String,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {

        var client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        var infoSource = infoSourceService.getInfoSourceByUuid(infoSourceUuid) ?: return notFoundMap("Info source")

        name?.let {
            infoSourceService.editInfoSourceName(client, infoSource, name, modifyValues = { modifiedClient, modifiedInfoSource -> client = modifiedClient; infoSource = modifiedInfoSource })
        }

        return okMap().add(infoSource)
    }

    @RequestMapping(value = "/trust/name/", method = arrayOf(RequestMethod.POST))
    fun trustVoteProduct(
            @RequestHeader clientUuid: String,
            @RequestParam infoSourceUuid: String,
            @RequestParam vote: TrustVote.TrustVoteType): HashMap<String, Any> {
        var client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        var infoSource = infoSourceService.getInfoSourceByUuid(infoSourceUuid) ?: return notFoundMap("Info source")

        infoSourceService.trustVoteInfoSourceName(client, infoSource, vote)

        return okMap().add(infoSource)
    }
}
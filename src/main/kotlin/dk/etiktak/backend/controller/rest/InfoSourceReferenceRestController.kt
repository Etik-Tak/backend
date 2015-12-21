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

import dk.etiktak.backend.controller.rest.json.addEntity
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.infosource.InfoSourceReferenceService
import dk.etiktak.backend.service.infosource.InfoSourceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/service/infosourcereference")
class InfoSourceReferenceRestController @Autowired constructor(
        private val infoSourceService: InfoSourceService,
        private val infoSourceReferenceService: InfoSourceReferenceService,
        private val infoChannelService: InfoChannelService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoChannelUuid: String,
            @RequestParam infoSourceUuid: String,
            @RequestParam url: String,
            @RequestParam title: String,
            @RequestParam summaryMarkdown: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid)
        client?.let {
            val infoChannel = infoChannelService.getInfoChannelByUuid(infoChannelUuid)
            infoChannel?.let {
                val infoSource = infoSourceService.getInfoSourceByUuid(infoSourceUuid)
                infoSource?.let {
                    val infoSourceReference = infoSourceReferenceService.createInfoSourceReference(
                            client, infoChannel, infoSource, url, title, summaryMarkdown)
                    return okMap().addEntity(infoSourceReference)
                }
            }
        }
        return notFoundMap()
    }
}
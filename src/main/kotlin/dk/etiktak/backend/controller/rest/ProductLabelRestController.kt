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
 * Rest controller responsible for handling product labels.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.security.CurrentlyLoggedClient
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductLabelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/product/label")
open class ProductLabelRestController @Autowired constructor(
        private val productLabelService: ProductLabelService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getProductLabel(
            @CurrentlyLoggedClient loggedClient: Client?,
            @RequestParam uuid: String): HashMap<String, Any> {

        val client = if (loggedClient != null) clientService.getByUuid(loggedClient.uuid) else null
        val productLabel = productLabelService.getProductLabelByUuid(uuid) ?: return notFoundMap("Product label")

        return okMap().add(productLabel, client, productLabelService)
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createProductLabel(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam name: String): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        val productLabel = productLabelService.createProductLabel(client, name)

        return okMap().add(productLabel, client, productLabelService)
    }

    @RequestMapping(value = "/edit/", method = arrayOf(RequestMethod.POST))
    fun editProductLabel(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productLabelUuid: String,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: return notFoundMap("Product label")

        name?.let {
            productLabelService.editProductLabelName(client, productLabel, name,
                    modifyValues = {modifiedClient, modifiedProductLabel -> client = modifiedClient; productLabel = modifiedProductLabel})
        }

        return okMap().add(productLabel, client, productLabelService)
    }

    @RequestMapping(value = "/trust/name/", method = arrayOf(RequestMethod.POST))
    fun trustVoteProductLabelName(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productLabelUuid: String,
            @RequestParam vote: TrustVote.TrustVoteType): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: return notFoundMap("Product label")

        productLabelService.trustVoteProductLabelName(client, productLabel, vote)

        return okMap().add(productLabel, client, productLabelService)
    }
}
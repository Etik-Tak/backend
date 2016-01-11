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
 * Rest controller responsible for handling product labels.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductLabelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/service/product/label")
class ProductLabelRestController @Autowired constructor(
        private val productLabelService: ProductLabelService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getProductLabel(
            @RequestParam uuid: String): HashMap<String, Any> {
        val productLabel = productLabelService.getProductLabelByUuid(uuid) ?: return notFoundMap("Product label")

        return productLabelOkMap(productLabel)
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createProductLabel(
            @RequestParam clientUuid: String,
            @RequestParam name: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")

        val productLabel = productLabelService.createProductLabel(client, name)

        return productLabelOkMap(productLabel)
    }

    fun productLabelOkMap(productLabel: ProductLabel): HashMap<String, Any> {
        return okMap()
                .add("productLabel", hashMapOf<String, Any>()
                        .add("uuid", productLabel.uuid)
                        .add("name", productLabel.name))
    }
}
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

import dk.etiktak.backend.controller.rest.json.JsonFilter
import dk.etiktak.backend.controller.rest.json.addEntity
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.infosource.InfoSourceReferenceService
import dk.etiktak.backend.service.infosource.InfoSourceService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/infosourcereference")
class InfoSourceReferenceRestController @Autowired constructor(
        private val infoSourceService: InfoSourceService,
        private val infoSourceReferenceService: InfoSourceReferenceService,
        private val productService: ProductService,
        private val productCategoryService: ProductCategoryService,
        private val productLabelService: ProductLabelService,
        private val infoChannelService: InfoChannelService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoChannelUuid: String,
            @RequestParam infoSourceUuid: String,
            @RequestParam url: String,
            @RequestParam title: String,
            @RequestParam summary: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val infoChannel = infoChannelService.getInfoChannelByUuid(infoChannelUuid) ?: return notFoundMap()
        val infoSource = infoSourceService.getInfoSourceByUuid(infoSourceUuid) ?: return notFoundMap()

        val infoSourceReference = infoSourceReferenceService.createInfoSourceReference(client, infoChannel, infoSource, url, title, summary)

        return okMap().addEntity(infoSourceReference, JsonFilter.CREATE)
    }

    @RequestMapping(value = "/assign/products/", method = arrayOf(RequestMethod.POST))
    fun assignProductsToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap()

        val products: MutableList<Product> = ArrayList()
        for (productUuid in productUuids) {
            val product = productService.getProductByUuid(productUuid) ?: continue
            products.add(product)
        }

        infoSourceReferenceService.assignProductsToInfoSourceReference(
                client,
                infoSourceReference,
                products)

        return okMap().addEntity(infoSourceReference, JsonFilter.CREATE)
    }

    @RequestMapping(value = "/assign/categories/", method = arrayOf(RequestMethod.POST))
    fun assignCategoriesToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productCategoryUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap()

        val productCategories: MutableList<ProductCategory> = ArrayList()
        for (productCategoryUuid in productCategoryUuids) {
            val productCategory = productCategoryService.getProductCategoryByUuid(productCategoryUuid) ?: continue
            productCategories.add(productCategory)
        }

        infoSourceReferenceService.assignProductCategoriesToInfoSourceReference(
                client,
                infoSourceReference,
                productCategories)

        return okMap().addEntity(infoSourceReference, JsonFilter.CREATE)
    }

    @RequestMapping(value = "/assign/labels/", method = arrayOf(RequestMethod.POST))
    fun assignLabelsToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productLabelUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap()

        val productLabels: MutableList<ProductLabel> = ArrayList()
        for (productLabelUuid in productLabelUuids) {
            val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: continue
            productLabels.add(productLabel)
        }

        infoSourceReferenceService.assignProductLabelsToInfoSourceReference(
                client,
                infoSourceReference,
                productLabels)

        return okMap().addEntity(infoSourceReference, JsonFilter.CREATE)
    }
}

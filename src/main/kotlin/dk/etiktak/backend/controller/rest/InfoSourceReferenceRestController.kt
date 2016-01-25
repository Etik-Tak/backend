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
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.company.CompanyService
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
        private val companyService: CompanyService,
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
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val infoChannel = infoChannelService.getInfoChannelByUuid(infoChannelUuid) ?: return notFoundMap("Info channel")
        val infoSource = infoSourceService.getInfoSourceByUuid(infoSourceUuid) ?: return notFoundMap("Info source")

        val infoSourceReference = infoSourceReferenceService.createInfoSourceReference(client, infoChannel, infoSource, url, title, summary)

        return infoSourceReferenceOkMap(infoSourceReference)
    }

    @RequestMapping(value = "/assign/products/", method = arrayOf(RequestMethod.POST))
    fun assignProductsToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap("Info source reference")

        val products: MutableList<Product> = ArrayList()
        for (productUuid in productUuids) {
            val product = productService.getProductByUuid(productUuid) ?: continue
            products.add(product)
        }

        infoSourceReferenceService.assignProductsToInfoSourceReference(
                client,
                infoSourceReference,
                products)

        return infoSourceReferenceOkMap(infoSourceReference)
    }

    @RequestMapping(value = "/assign/categories/", method = arrayOf(RequestMethod.POST))
    fun assignCategoriesToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productCategoryUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap("Info source reference")

        val productCategories: MutableList<ProductCategory> = ArrayList()
        for (productCategoryUuid in productCategoryUuids) {
            val productCategory = productCategoryService.getProductCategoryByUuid(productCategoryUuid) ?: continue
            productCategories.add(productCategory)
        }

        infoSourceReferenceService.assignProductCategoriesToInfoSourceReference(
                client,
                infoSourceReference,
                productCategories)

        return infoSourceReferenceOkMap(infoSourceReference)
    }

    @RequestMapping(value = "/assign/labels/", method = arrayOf(RequestMethod.POST))
    fun assignLabelsToInfoSourceReference(
            @RequestParam clientUuid: String,
            @RequestParam infoSourceReferenceUuid: String,
            @RequestParam productLabelUuids: List<String>): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val infoSourceReference = infoSourceReferenceService.getInfoSourceReferenceByUuid(infoSourceReferenceUuid) ?: return notFoundMap("Info source reference")

        val productLabels: MutableList<ProductLabel> = ArrayList()
        for (productLabelUuid in productLabelUuids) {
            val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: continue
            productLabels.add(productLabel)
        }

        infoSourceReferenceService.assignProductLabelsToInfoSourceReference(
                client,
                infoSourceReference,
                productLabels)

        return infoSourceReferenceOkMap(infoSourceReference)
    }

    fun infoSourceReferenceOkMap(infoSourceReference: InfoSourceReference): HashMap<String, Any> {
        return okMap()
                .add("infoSourceReference", hashMapOf<String, Any>()
                        .add("uuid", infoSourceReference.uuid)
                        .add("url", infoSourceReference.url)
                        .add("title", infoSourceReference.title)
                        .add("summary", infoSourceReference.summary)
                        .add("categories", infoSourceReference.productCategories, { category -> hashMapOf<String, Any>()
                                .add("uuid", category.uuid)
                                .add("name", category.name) })
                        .add("labels", infoSourceReference.productLabels, { label -> hashMapOf<String, Any>()
                                .add("uuid", label.uuid)
                                .add("name", label.name) })
                        .add("companies", infoSourceReference.companies, { company -> hashMapOf<String, Any>()
                                .add("uuid", company.uuid)
                                .add("name", company.name) }))
    }
}

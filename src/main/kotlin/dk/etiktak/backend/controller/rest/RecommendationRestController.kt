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
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.recommendation.RecommendationScore
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.company.CompanyService
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.infosource.InfoSourceReferenceService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import dk.etiktak.backend.service.product.ProductTagService
import dk.etiktak.backend.service.recommendation.RecommendationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/service/recommendation")
class RecommendationRestController @Autowired constructor(
        private val recommendationService: RecommendationService,
        private val productService: ProductService,
        private val productCategoryService: ProductCategoryService,
        private val productLabelService: ProductLabelService,
        private val productTagService: ProductTagService,
        private val companyService: CompanyService,
        private val infoChannelService: InfoChannelService,
        private val infoSourceReferenceService: InfoSourceReferenceService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getRecommendation(
            @RequestParam clientUuid: String,
            @RequestParam productUuid: String): HashMap<String, Any> {

        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")

        val recommendations = recommendationService.getRecommendations(client, product)

        return okMap().add(recommendations)
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createRecommendation(
            @RequestParam clientUuid: String,
            @RequestParam infoChannelUuid: String,
            @RequestParam summary: String,
            @RequestParam score: String,
            @RequestParam(required = false) infoSourceReferenceUrlList: List<String>,
            @RequestParam(required = false) productUuid: String? = null,
            @RequestParam(required = false) productCategoryUuid: String? = null,
            @RequestParam(required = false) productLabelUuid: String? = null,
            @RequestParam(required = false) productTagUuid: String? = null,
            @RequestParam(required = false) companyUuid: String? = null): HashMap<String, Any> {

        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val infoChannel = infoChannelService.getInfoChannelByUuid(infoChannelUuid) ?: return notFoundMap("Info channel")

        val scoreType = RecommendationScore.valueOf(score)

        // Create product recommendation
        productUuid?.let {
            val product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
            val recommendation = recommendationService.createRecommendation(client, infoChannel, summary, scoreType, product, infoSourceReferenceUrlList)
            return okMap().add(recommendation)
        }

        // Create product category recommendation
        productCategoryUuid?.let {
            val productCategory = productCategoryService.getProductCategoryByUuid(productCategoryUuid) ?: return notFoundMap("Product category")
            val recommendation = recommendationService.createRecommendation(client, infoChannel, summary, scoreType, productCategory, infoSourceReferenceUrlList)
            return okMap().add(recommendation)
        }

        // Create product label recommendation
        productLabelUuid?.let {
            val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: return notFoundMap("Product label")
            val recommendation = recommendationService.createRecommendation(client, infoChannel, summary, scoreType, productLabel, infoSourceReferenceUrlList)
            return okMap().add(recommendation)
        }

        // Create product tag recommendation
        productTagUuid?.let {
            val productTag = productTagService.getProductTagByUuid(productTagUuid) ?: return notFoundMap("Product tag")
            val recommendation = recommendationService.createRecommendation(client, infoChannel, summary, scoreType, productTag, infoSourceReferenceUrlList)
            return okMap().add(recommendation)
        }

        // Create company recommendation
        companyUuid?.let {
            val company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")
            val recommendation = recommendationService.createRecommendation(client, infoChannel, summary, scoreType, company, infoSourceReferenceUrlList)
            return okMap().add(recommendation)
        }

        return illegalInvocationMap("None of the required parameters set")
    }
}

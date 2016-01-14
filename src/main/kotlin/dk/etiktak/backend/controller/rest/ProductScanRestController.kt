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
 * Rest controller responsible for handling product scans.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.location.Location
import dk.etiktak.backend.model.recommendation.CompanyRecommendation
import dk.etiktak.backend.model.recommendation.ProductCategoryRecommendation
import dk.etiktak.backend.model.recommendation.ProductLabelRecommendation
import dk.etiktak.backend.model.recommendation.ProductRecommendation
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/service/product/scan")
class ProductScanRestController @Autowired constructor(
        private val productService: ProductService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.POST))
    fun scanProduct(
            @RequestParam barcode: String,
            @RequestParam clientUuid: String,
            @RequestParam(required = false) latitude: Double? = null,
            @RequestParam(required = false) longitude: Double? = null): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")

        val productScanResult = productService.scanProduct(client, barcode, latitude, longitude)

        return okMap()
                .add("scan", hashMapOf<String, Any>()
                        .add("uuid", productScanResult.productScan.uuid)
                        .add("product", hashMapOf<String, Any>()
                                .add("uuid", productScanResult.product.uuid)
                                .add("name", productScanResult.product.name)
                                .add("correctnessTrust", productScanResult.product.correctnessTrust)
                                .add("categories", productScanResult.product.productCategories, { category -> hashMapOf<String, Any>()
                                        .add("uuid", category.uuid)
                                        .add("name", category.name) })
                                .add("labels", productScanResult.product.productLabels, { label -> hashMapOf<String, Any>()
                                        .add("uuid", label.uuid)
                                        .add("name", label.name) }))
                        .add("recommendations", productScanResult.recommendations, { recommendation -> hashMapOf<String, Any>()
                                .add("uuid", recommendation.uuid)
                                .add("summary", recommendation.summary)
                                .add("score", recommendation.score.name)
                                .add("product", if (recommendation.javaClass == ProductRecommendation::class.java) (recommendation as ProductRecommendation).product.uuid else null)
                                .add("productCategory", if (recommendation.javaClass == ProductCategoryRecommendation::class.java) (recommendation as ProductCategoryRecommendation).productCategory.uuid else null)
                                .add("productLabel", if (recommendation.javaClass == ProductLabelRecommendation::class.java) (recommendation as ProductLabelRecommendation).productLabel.uuid else null)
                                .add("company", if (recommendation.javaClass == CompanyRecommendation::class.java) (recommendation as CompanyRecommendation).company.uuid else null) })
                )
    }

    @RequestMapping(value = "/assign/location/", method = arrayOf(RequestMethod.POST))
    fun provideProductScanLocation(
            @RequestParam clientUuid: String,
            @RequestParam productScanUuid: String,
            @RequestParam latitude: Double,
            @RequestParam longitude: Double): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        var productScan = productService.getProductScanByUuid(productScanUuid) ?: return notFoundMap("Product")

        productService.assignLocationToProductScan(client, productScan, latitude, longitude, {scan -> productScan = scan})

        return okMap()
                .add("scan", hashMapOf<String, Any>()
                        .add("uuid", productScan.uuid)
                )
    }
}
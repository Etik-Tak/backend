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
 * Rest controller responsible for handling product lifecycle.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.JsonFilter
import dk.etiktak.backend.controller.rest.json.addEntity
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/product")
class ProductRestController @Autowired constructor(
        private val productService: ProductService,
        private val productCategoryService: ProductCategoryService,
        private val productLabelService: ProductLabelService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getProduct(
            @RequestParam(required = false) uuid: String?,
            @RequestParam(required = false) barcode: String?): HashMap<String, Any> {
        var product: Product? = null
        if (!StringUtils.isEmpty(uuid)) {
            product = productService.getProductByUuid(uuid!!)
        }
        if (!StringUtils.isEmpty(barcode)) {
            product = productService.getProductByBarcode(barcode!!)
        }
        product?.let {
            return okMap().addEntity(product, JsonFilter.RETRIEVE)
        }
        return notFoundMap()
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.GET))
    fun createProduct(
            @RequestParam clientUuid: String,
            @RequestParam name: String,
            @RequestParam(required = false) barcode: String?,
            @RequestParam(required = false) barcodeType: String?,
            @RequestParam(required = false) categoryUuidList: List<String>?,
            @RequestParam(required = false) labelUuidList: List<String>?): HashMap<String, Any> {

        val client = clientService.getByUuid(clientUuid)
        client?.let {

            // List of product categories
            val productCategories = ArrayList<ProductCategory>()
            categoryUuidList?.let {
                for (productCategoryUuid in categoryUuidList) {
                    val productCategory = productCategoryService.getProductCategoryByUuid(productCategoryUuid)
                    productCategory?.let {
                        productCategories.add(productCategory)
                    }
                }
            }

            // List of product labels
            val productLabels = ArrayList<ProductLabel>()
            labelUuidList?.let {
                for (productLabelUuid in labelUuidList) {
                    val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid)
                    productLabel?.let {
                        productLabels.add(productLabel)
                    }
                }
            }

            // Create product
            val product = productService.createProduct(
                    client,
                    barcode,
                    if (barcodeType != null) Product.BarcodeType.valueOf(barcodeType) else null,
                    name,
                    productCategories,
                    productLabels)
            return okMap().addEntity(product, JsonFilter.CREATE)
        }
        return notFoundMap()
    }
}

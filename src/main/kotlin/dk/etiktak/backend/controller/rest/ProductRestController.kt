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

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
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
            @RequestParam(required = false) uuid: String? = null,
            @RequestParam(required = false) barcode: String? = null): HashMap<String, Any> {
        uuid?.let {
            val product = productService.getProductByUuid(uuid) ?: return notFoundMap()
            return productOkMap(product)
        }
        barcode?.let {
            val product = productService.getProductByBarcode(barcode) ?: return notFoundMap()
            return productOkMap(product)
        }
        return notFoundMap()
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createProduct(
            @RequestParam clientUuid: String,
            @RequestParam name: String,
            @RequestParam(required = false) barcode: String? = null,
            @RequestParam(required = false) barcodeType: String? = null,
            @RequestParam(required = false) categoryUuidList: List<String>? = null,
            @RequestParam(required = false) labelUuidList: List<String>? = null): HashMap<String, Any> {

        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()

        // List of product categories
        val productCategories = ArrayList<ProductCategory>()
        categoryUuidList?.let {
            for (productCategoryUuid in categoryUuidList) {
                val productCategory = productCategoryService.getProductCategoryByUuid(productCategoryUuid) ?: continue
                productCategories.add(productCategory)
            }
        }

        // List of product labels
        val productLabels = ArrayList<ProductLabel>()
        labelUuidList?.let {
            for (productLabelUuid in labelUuidList) {
                val productLabel = productLabelService.getProductLabelByUuid(productLabelUuid) ?: continue
                productLabels.add(productLabel)
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

        return productOkMap(product)
    }

    @RequestMapping(value = "/assign/category/", method = arrayOf(RequestMethod.POST))
    fun assignCategoryToProduct(
            @RequestParam clientUuid: String,
            @RequestParam productUuid: String,
            @RequestParam categoryUuid: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val product = productService.getProductByUuid(productUuid) ?: return notFoundMap()
        val productCategory = productCategoryService.getProductCategoryByUuid(categoryUuid) ?: return notFoundMap()

        productService.assignCategoryToProduct(client, product, productCategory)

        return okMap()
    }

    @RequestMapping(value = "/assign/label/", method = arrayOf(RequestMethod.POST))
    fun assignLabelToProduct(
            @RequestParam clientUuid: String,
            @RequestParam productUuid: String,
            @RequestParam labelUuid: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        val product = productService.getProductByUuid(productUuid) ?: return notFoundMap()
        val productLabel = productLabelService.getProductLabelByUuid(labelUuid) ?: return notFoundMap()

        productService.assignLabelToProduct(client, product, productLabel)

        return okMap()
    }

    fun productOkMap(product: Product): HashMap<String, Any> {
        return okMap()
                .add("product", hashMapOf<String, Any>()
                        .add("uuid", product.uuid)
                        .add("name", product.name)
                        .add("categories", product.productCategories, { category -> hashMapOf<String, Any>()
                                .add("uuid", category.uuid)
                                .add("name", category.name) })
                        .add("labels", product.productLabels, { label -> hashMapOf<String, Any>()
                                .add("uuid", label.uuid)
                                .add("name", label.name) }))
    }
}

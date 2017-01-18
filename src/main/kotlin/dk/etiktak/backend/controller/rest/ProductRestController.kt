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
 * Rest controller responsible for handling product lifecycle.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductTag
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.security.CurrentlyLoggedClient
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.company.CompanyService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import dk.etiktak.backend.service.product.ProductTagService
import org.slf4j.LoggerFactory
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
        private val productTagService: ProductTagService,
        private val companyService: CompanyService,
        private val clientService: ClientService) : BaseRestController() {

    private val logger = LoggerFactory.getLogger(ProductRestController::class.java)

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getProduct(
            @RequestParam(required = false) uuid: String? = null,
            @RequestParam(required = false) barcode: String? = null): HashMap<String, Any> {

        uuid?.let {
            val product = productService.getProductByUuid(uuid) ?: return notFoundMap("Product")
            return okMap().add(product, client = null, productService = productService, companyService = companyService)
        }

        barcode?.let {
            val product = productService.getProductByBarcode(barcode) ?: return notFoundMap("Product")
            return okMap().add(product, client = null, productService = productService, companyService = companyService)
        }

        return notFoundMap("Product")
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam name: String,
            @RequestParam(required = false) barcode: String,
            @RequestParam(required = false) barcodeType: String? = null,
            @RequestParam(required = false) categoryUuidList: List<String>? = null,
            @RequestParam(required = false) labelUuidList: List<String>? = null,
            @RequestParam(required = false) tagUuidList: List<String>? = null,
            @RequestParam(required = false) companyUuidList: List<String>?): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        // List of product categories
        val productCategories = ArrayList<ProductCategory>()
        categoryUuidList?.let {
            categoryUuidList.mapNotNullTo(productCategories) { productCategoryService.getProductCategoryByUuid(it) }
        }

        // List of product labels
        val productLabels = ArrayList<ProductLabel>()
        labelUuidList?.let {
            labelUuidList.mapNotNullTo(productLabels) { productLabelService.getProductLabelByUuid(it) }
        }

        // List of product tags
        val productTags = ArrayList<ProductTag>()
        tagUuidList?.let {
            tagUuidList.mapNotNullTo(productTags) { productTagService.getProductTagByUuid(it) }
        }

        // List of companies
        val companies = ArrayList<Company>()
        companyUuidList?.let {
            companyUuidList.mapNotNullTo(companies) { companyService.getCompanyByUuid(it) }
        }

        // Create product
        val product = productService.createProduct(
                client,
                barcode,
                if (barcodeType != null) Product.BarcodeType.valueOf(barcodeType) else null,
                name,
                productCategories,
                productLabels,
                productTags,
                companies)

        return okMap().add(product, client, productService, companyService)
    }

    @RequestMapping(value = "/edit/", method = arrayOf(RequestMethod.POST))
    fun editProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")

        name?.let {
            productService.editProductName(client, product, name, modifyValues = { modifiedClient, modifiedProduct -> client = modifiedClient; product = modifiedProduct })
        }

        return okMap().add(product, client, productService, companyService)
    }

    @RequestMapping(value = "/assign/category/", method = arrayOf(RequestMethod.POST))
    fun assignCategoryToProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam categoryUuid: String): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
        var productCategory = productCategoryService.getProductCategoryByUuid(categoryUuid) ?: return notFoundMap("Product category")

        productService.assignCategoryToProduct(client, product, productCategory,
                modifyValues = {modifiedClient, modifiedProduct, modifiedProductCategory -> client = modifiedClient; product = modifiedProduct; productCategory = modifiedProductCategory})

        return okMap().add(product, client, productService, companyService)
    }

    @RequestMapping(value = "/assign/label/", method = arrayOf(RequestMethod.POST))
    fun assignLabelToProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam labelUuid: String): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
        var productLabel = productLabelService.getProductLabelByUuid(labelUuid) ?: return notFoundMap("Product label")

        productService.assignLabelToProduct(client, product, productLabel,
                modifyValues = { modifiedClient, modifiedProduct, modifiedProductLabel -> client = modifiedClient; product = modifiedProduct; productLabel = modifiedProductLabel})

        return okMap().add(product, client, productService, companyService)
    }

    @RequestMapping(value = "/assign/tag/", method = arrayOf(RequestMethod.POST))
    fun assignTagToProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam tagUuid: String): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
        var productTag = productTagService.getProductTagByUuid(tagUuid) ?: return notFoundMap("Product tag")

        productService.assignTagToProduct(client, product, productTag,
                modifyValues = { modifiedClient, modifiedProduct, modifiedProductTag -> client = modifiedClient; product = modifiedProduct; productTag = modifiedProductTag})

        return okMap().add(product, client, productService, companyService)
    }

    @RequestMapping(value = "/assign/company/", method = arrayOf(RequestMethod.POST))
    fun assignCompanyToProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam(required = false) companyUuid: String?,
            @RequestParam(required = false) companyName: String?): HashMap<String, Any> {

        // Check parameters
        if (StringUtils.isEmpty(companyUuid) && StringUtils.isEmpty(companyName)) {
            return illegalInvocationMap("Either 'companyUuid' or 'companyName' must be provided")
        }

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
        var company: Company? = null

        // Find company by UUID
        companyUuid?.let {
            company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")
        }

        // Find company by name
        companyName?.let {
            company = companyService.getCompanyByName(companyName)

            // Create new company
            if (company == null) {
                logger.info("Creating new company with name $companyName")
                company = companyService.createCompany(client, companyName, modifyValues = { modifiedClient -> client = modifiedClient })
            }
        }

        // Check that we indeed have a company
        if (company == null) {
            return notFoundMap("Company")
        }

        // Assign company
        productService.assignCompanyToProduct(client, product, company!!,
                modifyValues = { modifiedClient, modifiedProduct, modifiedCompany -> client = modifiedClient; product = modifiedProduct; company = modifiedCompany })

        return okMap()
                .add(product, client, productService, companyService)
                .add(company!!, client, companyService)
    }

    @RequestMapping(value = "/remove/company/", method = arrayOf(RequestMethod.POST))
    fun removeCompanyToProduct(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam companyUuid: String): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")
        var company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")

        productService.removeCompanyFromProduct(client, product, company,
                modifyValues = { modifiedClient, modifiedProduct, modifiedCompany -> client = modifiedClient; product = modifiedProduct; company = modifiedCompany })

        return okMap()
                .add(product, client, productService, companyService)
                .add(company, client, companyService)
    }

    @RequestMapping(value = "/trust/name/", method = arrayOf(RequestMethod.POST))
    fun trustVoteProductName(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam productUuid: String,
            @RequestParam vote: TrustVote.TrustVoteType): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        val product = productService.getProductByUuid(productUuid) ?: return notFoundMap("Product")

        productService.trustVoteProductName(client, product, vote)

        return okMap().add(product, client, productService, companyService)
    }
}

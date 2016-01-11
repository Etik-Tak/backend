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

package dk.etiktak.backend.service.product

import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.product.*
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.trust.ProductTrustVote
import dk.etiktak.backend.model.trust.TrustVoteType
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.*
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.recommendation.RecommendationService
import dk.etiktak.backend.service.security.SecurityService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.util.*

@Service
@Transactional
open class ProductService @Autowired constructor(
        private val productRepository: ProductRepository,
        private val recommendationService: RecommendationService,
        private val productScanRepository: ProductScanRepository,
        private val clientRepository: ClientRepository,
        private val locationRepository: LocationRepository,
        private val productCategoryRepository: ProductCategoryRepository,
        private val productLabelRepository: ProductLabelRepository,
        private val companyRepository: CompanyRepository,
        private val trustVoteRepository: TrustVoteRepository,
        private val securityService: SecurityService) {

    private val logger = LoggerFactory.getLogger(ProductService::class.java)

    /**
     * Finds a product from the given UUID.
     *
     * @param uuid     UUID
     * @return         Product with given UUID
     */
    open fun getProductByUuid(uuid: String): Product? {
        return productRepository.findByUuid(uuid)
    }

    /**
     * Finds a product from the given barcode.
     *
     * @param barcode   Barcode
     * @return          Product with given barcode
     */
    open fun getProductByBarcode(barcode: String): Product? {
        return productRepository.findByBarcode(barcode)
    }

    /**
     * Finds a product scan from the given uuid.
     *
     * @param uuid  UUID
     * @return      Product scan with given UUID
     */
    open fun getProductScanByUuid(uuid: String): ProductScan? {
        return productScanRepository.findByUuid(uuid)
    }

    /**
     * Creates a new product.
     *
     * @param client        Client
     * @param barcode       Barcode
     * @param barcodeType   Barcode type
     * @param name          Name of product
     * @param categories    Product categories
     * @param labels        Product labels
     * @param companies     Companies
     * @param modifyValues  Function called with modified client, product categories, product labels and companies
     * @return              Created product
     */
    open fun createProduct(client: Client, barcode: String?, barcodeType: Product.BarcodeType?, name: String,
                           categories: List<ProductCategory>, labels: List<ProductLabel>, companies: List<Company>,
                           modifyValues: (Client, List<ProductCategory>, List<ProductLabel>, List<Company>) -> Unit = {client, productCategories, productLabels, companies -> Unit}): Product {

        val product = Product()
        product.uuid = CryptoUtil().uuid()
        product.creator = client
        product.name = name
        product.productCategories.addAll(categories)
        product.productLabels.addAll(labels)
        product.companies.addAll(companies)

        // Glue it together
        client.products.add(product)
        for (productCategory in categories) {
            productCategory.products.add(product)
        }
        for (productLabel in labels) {
            productLabel.products.add(product)
        }
        for (company in companies) {
            company.products.add(product)
        }

        // Save it all
        val modifiedProductCategories: MutableList<ProductCategory> = ArrayList()
        for (productCategory in categories) {
            modifiedProductCategories.add(productCategoryRepository.save(productCategory))
        }
        val modifiedProductLabels: MutableList<ProductLabel> = ArrayList()
        for (productLabel in labels) {
            modifiedProductLabels.add(productLabelRepository.save(productLabel))
        }
        val modifiedCompanies: MutableList<Company> = ArrayList()
        for (company in companies) {
            modifiedCompanies.add(companyRepository.save(company))
        }
        var modifiedClient = clientRepository.save(client)
        var modifiedProduct = productRepository.save(product)

        // Assign barcode
        if (barcode != null) {
            assignBarcodeToProduct(modifiedClient, modifiedProduct, barcode, barcodeType ?: Product.BarcodeType.UNKNOWN, { product -> modifiedProduct = product})
        }

        // Trust vote product
        trustVoteProduct(modifiedClient, modifiedProduct, TrustVoteType.Trusted, modifyValues = { client, product -> modifiedClient = client; modifiedProduct = product})

        modifyValues(modifiedClient, modifiedProductCategories, modifiedProductLabels, modifiedCompanies)

        return modifiedProduct
    }

    /**
     * Assign barcode to product.
     *
     * @param client        Client
     * @param product       Product
     * @param barcode       Barcode
     * @param barcodeType   Barcode type
     * @param modifyValues  Function called with modified product
     */
    open fun assignBarcodeToProduct(client: Client, product: Product, barcode: String, barcodeType: Product.BarcodeType, modifyValues: (Product) -> Unit = {}) {
        product.barcode = barcode
        product.barcodeType = barcodeType

        val modifiedProduct = productRepository.save(product)

        modifyValues(modifiedProduct)
    }

    /**
     * Assigns a category to a product.
     *
     * @param client            Client
     * @param product           Product
     * @param productCategory   Product category
     * @param modifyValues      Function called with modified product and product category
     */
    open fun assignCategoryToProduct(client: Client, product: Product, productCategory: ProductCategory, modifyValues: (Product, ProductCategory) -> Unit = {product, productCategory -> Unit}) {
        securityService.assertCreatorOrAdmin(client, product.creator)

        // Glue it together
        product.productCategories.add(productCategory)
        productCategory.products.add(product)

        // Save it all
        val modifiedProductCategory = productCategoryRepository.save(productCategory)
        val modifiedProduct = productRepository.save(product)

        modifyValues(modifiedProduct, modifiedProductCategory)
    }

    /**
     * Assigns a label to a product.
     *
     * @param client            Client
     * @param product           Product
     * @param productLabel      Product label
     * @param modifyValues      Function called with modified product and product label
     */
    open fun assignLabelToProduct(client: Client, product: Product, productLabel: ProductLabel, modifyValues: (Product, ProductLabel) -> Unit = {product, productLabel -> Unit}) {
        securityService.assertCreatorOrAdmin(client, product.creator)

        // Glue it together
        product.productLabels.add(productLabel)
        productLabel.products.add(product)

        // Save it all
        val modifiedProductLabel = productLabelRepository.save(productLabel)
        val modifiedProduct = productRepository.save(product)

        modifyValues(modifiedProduct, modifiedProductLabel)
    }

    /**
     * Finds a product from the given barcode and creates and returns a product scan. If no product is found,
     * the methods creates an empty product.
     *
     * @param barcode     Barcode
     * @param client      Client
     * @param location    Optional location
     * @return            Created product scan entry (which contains the actual product)
     */
    open fun scanProduct(barcode: String, client: Client, location: Location?): ProductScanResult {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(barcode),
                "Barcode must be provided")

        Assert.notNull(
                client,
                "Client must be provided")

        // Find product
        var modifiedClient = client

        var product = getProductByBarcode(barcode)
        if (product == null) {
            product = createProduct(
                    client, barcode, Product.BarcodeType.UNKNOWN, "Automatisk oprettet",
                    categories = arrayListOf(), labels = arrayListOf(), companies = arrayListOf(),
                    modifyValues = {client, productCategories, productLabels, companies -> modifiedClient = client})
        }

        // Create product scan
        var modifiedProduct = product

        val productScan = createProductScan(modifiedProduct, client, location,
                modifyValues = {product, client, location -> modifiedProduct = product; modifiedClient = client})

        // Retrieve recommendations
        val recommendations = recommendationService.getRecommendations(modifiedClient, product)

        return ProductScanResult(productScan, product, recommendations)
    }

    /**
     * Assigns a location to an already created product scan. Fails if location already assigned.
     *
     * @param client         Client
     * @param productScan    Product scan entry
     * @param location       Location
     * @param modifyValues   Function called with modified product scan
     */
    open fun assignLocationToProductScan(client: Client, productScan: ProductScan, location: Location?, modifyValues: (ProductScan) -> Unit = {}) {
        securityService.assertCreatorOrAdmin(client, productScan.client)

        Assert.notNull(
                location,
                "Location must not be empty")

        Assert.isNull(
                productScan.location,
                "Location already set on product scan with UUID: " + productScan.uuid)

        productScan.location = location
        val modifiedProductScan = productScanRepository.save(productScan)

        modifyValues(modifiedProductScan)
    }


    /**
     * Creates a product scan, glues it together with product and client, and saves it all.
     *
     * @param product        Product
     * @param client         Client
     * @param location       Optional location
     * @param modifyValues   Function called with modified product, client and location
     * @return               Product scan
     */
    open fun createProductScan(product: Product, client: Client, location: Location?,
                               modifyValues: (Product, Client, Location?) -> Unit = {product, client, location -> Unit}): ProductScan {

        // Create product scan
        val productScan = ProductScan()
        productScan.uuid = CryptoUtil().uuid()
        productScan.product = product
        productScan.client = client
        if (location != null) {
            productScan.location = location
        }

        // Glue together with client and product
        client.productScans.add(productScan)
        product.productScans.add(productScan)

        // Save if all
        var modifiedLocation: Location? = null
        if (location != null) {
            modifiedLocation = locationRepository.save(location)
        }
        val modifiedProductScan = productScanRepository.save(productScan)
        val modifiedProduct = productRepository.save(product)
        val modifiedClient = clientRepository.save(client)

        modifyValues(modifiedProduct, modifiedClient, modifiedLocation)

        return modifiedProductScan
    }

    /**
     * Trust vote product correctness.
     *
     * @param client        Client
     * @param product       Product
     * @param vote          Vote
     * @param modifyValues  Function called with modified client and product
     */
    open fun trustVoteProduct(client: Client, product: Product, vote: TrustVoteType, modifyValues: (Client, Product) -> Unit = {client, product -> Unit}): ProductTrustVote {
        val productTrustVote = ProductTrustVote()
        productTrustVote.client = client
        productTrustVote.product = product
        productTrustVote.vote = vote

        product.correctnessTrustVotes.add(productTrustVote)
        client.productTrustVotes.add(productTrustVote)

        val modifiedProductTrustVote = trustVoteRepository.save(productTrustVote)
        val modifiedProduct = productRepository.save(product)
        val modifiedClient = clientRepository.save(client)

        modifyValues(modifiedClient, modifiedProduct)

        return modifiedProductTrustVote
    }
}

data class ProductScanResult(
        val productScan: ProductScan,
        val product: Product,
        val recommendations: List<Recommendation>)

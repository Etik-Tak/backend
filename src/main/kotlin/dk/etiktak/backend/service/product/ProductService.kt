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
import dk.etiktak.backend.model.location.Location
import dk.etiktak.backend.model.product.*
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.trust.TrustItem
import dk.etiktak.backend.model.trust.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.*
import dk.etiktak.backend.repository.trust.TrustItemRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.recommendation.RecommendationService
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.TrustService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
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
        private val trustItemRepository: TrustItemRepository,
        private val trustService: TrustService) {

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
    @ClientVerified
    open fun createProduct(client: Client, barcode: String, barcodeType: Product.BarcodeType?, name: String,
                           categories: List<ProductCategory> = listOf(),
                           labels: List<ProductLabel> = listOf(),
                           companies: List<Company> = listOf(),
                           modifyValues: (Client, List<ProductCategory>, List<ProductLabel>, List<Company>) -> Unit = {client, productCategories, productLabels, companies -> Unit}): Product {

        val product = Product()
        product.uuid = CryptoUtil().uuid()
        product.name = name
        product.barcode = barcode
        product.barcodeType = barcodeType ?: Product.BarcodeType.UNKNOWN
        product.productCategories.addAll(categories)
        product.productLabels.addAll(labels)
        product.companies.addAll(companies)

        // Create trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Glue it together
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

        modifiedClient = clientRepository.save(modifiedClient)
        var modifiedProduct = productRepository.save(product)

        modifyValues(modifiedClient, modifiedProductCategories, modifiedProductLabels, modifiedCompanies)

        // Update trust
        trustService.updateTrust(trustItem)

        return modifiedProduct
    }

    /**
     * Creates a new empty product.
     *
     * @param client        Client
     * @param barcode       Barcode
     * @param barcodeType   Barcode type
     * @param modifyValues  Function called with modified client
     * @return              Created product
     */
    open fun createEmptyProduct(client: Client, barcode: String, barcodeType: Product.BarcodeType?,
                                modifyValues: (Client) -> Unit = {}): Product {

        val product = Product()
        product.uuid = CryptoUtil().uuid()
        product.name = ""
        product.barcode = barcode
        product.barcodeType = barcodeType ?: Product.BarcodeType.UNKNOWN

        // Create trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Save it all
        modifiedClient = clientRepository.save(modifiedClient)
        var modifiedProduct = productRepository.save(product)

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient)

        return modifiedProduct
    }

    /**
     * Edits a product.
     *
     * @param client        Client
     * @param product       Product
     * @param name          Name of product
     * @param modifyValues  Function called with modified client and product
     */
    @ClientVerified
    open fun editProduct(client: Client, product: Product, name: String?, modifyValues: (Client, Product) -> Unit = {client, product -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, productTrustItem(product))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Modify fields
        product.name = name ?: product.name

        // Save it all
        modifiedClient = clientRepository.save(modifiedClient)
        var modifiedProduct = productRepository.save(product)

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient, modifiedProduct)
    }

    /**
     * Assigns a category to a product.
     *
     * @param client            Client
     * @param product           Product
     * @param productCategory   Product category
     * @param modifyValues      Function called with modified client, product and product category
     */
    @ClientVerified
    open fun assignCategoryToProduct(client: Client, product: Product, productCategory: ProductCategory, modifyValues: (Client, Product, ProductCategory) -> Unit = {client, product, productCategory -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, productTrustItem(product))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Add product category
        product.productCategories.add(productCategory)
        productCategory.products.add(product)

        // Save it all
        var modifiedProductCategory = productCategoryRepository.save(productCategory)
        var modifiedProduct = productRepository.save(product)

        modifyValues(modifiedClient, modifiedProduct, modifiedProductCategory)
    }

    /**
     * Assigns a label to a product.
     *
     * @param client            Client
     * @param product           Product
     * @param productLabel      Product label
     * @param modifyValues      Function called with modified client, product and product label
     */
    @ClientVerified
    open fun assignLabelToProduct(client: Client, product: Product, productLabel: ProductLabel, modifyValues: (Client, Product, ProductLabel) -> Unit = {client, product, productLabel -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, productTrustItem(product))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Add product label
        product.productLabels.add(productLabel)
        productLabel.products.add(product)

        // Save it all
        var modifiedProductLabel = productLabelRepository.save(productLabel)
        var modifiedProduct = productRepository.save(product)

        modifyValues(modifiedClient, modifiedProduct, modifiedProductLabel)
    }

    /**
     * Assigns a company to a product.
     *
     * @param client            Client
     * @param product           Product
     * @param company           Company
     * @param modifyValues      Function called with modified client, product and product label
     */
    @ClientVerified
    open fun assignCompanyToProduct(client: Client, product: Product, company: Company, modifyValues: (Client, Product, Company) -> Unit = {client, product, company -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, productTrustItem(product))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Product, product.uuid, modifyValues = {client -> modifiedClient = client})
        product.trustItemUuid = trustItem.uuid

        // Add company
        product.companies.add(company)
        company.products.add(product)

        // Save it all
        var modifiedCompany = companyRepository.save(company)
        var modifiedProduct = productRepository.save(product)

        modifyValues(modifiedClient, modifiedProduct, modifiedCompany)
    }

    /**
     * Finds a product from the given barcode and creates and returns a product scan. If no product is found,
     * the methods creates an empty product.
     *
     * @param client      Client
     * @param barcode     Barcode
     * @param latitude    Optional latitude
     * @param longitude   Optional longitude
     * @return            Created product scan entry (which contains the actual product)
     */
    @ClientValid
    open fun scanProduct(client: Client, barcode: String, latitude: Double?, longitude: Double?): ProductScanResult {

        // Find product
        var modifiedClient = client

        var product = getProductByBarcode(barcode)
        if (product == null) {
            product = createEmptyProduct(client, barcode, Product.BarcodeType.UNKNOWN, modifyValues = {client -> modifiedClient = client})
        }

        // Create product scan
        var modifiedProduct = product

        val productScan = createProductScan(client, modifiedProduct, latitude, longitude,
                modifyValues = {client, product -> modifiedClient = client; modifiedProduct = product})

        // Retrieve recommendations
        val recommendations = recommendationService.getRecommendations(modifiedClient, product)

        return ProductScanResult(productScan, product, recommendations)
    }

    /**
     * Assigns a location to an already created product scan. Fails if location already assigned.
     *
     * @param client         Client
     * @param productScan    Product scan entry
     * @param latitude       Latitude
     * @param longitude      Longitude
     * @param modifyValues   Function called with modified product scan
     */
    @ClientValid
    open fun assignLocationToProductScan(client: Client, productScan: ProductScan, latitude: Double, longitude: Double, modifyValues: (ProductScan) -> Unit = {}) {
        Assert.isTrue(
                client.uuid.equals(productScan.client.uuid),
                "Client with UUID ${client.uuid} not owner of object. Owner has UUID ${productScan.client.uuid}"
        )

        Assert.isNull(
                productScan.location,
                "Location already set on product scan with UUID: " + productScan.uuid)

        val location = Location(latitude, longitude)
        productScan.location = location

        locationRepository.save(location)

        val modifiedProductScan = productScanRepository.save(productScan)

        modifyValues(modifiedProductScan)
    }

    /**
     * Creates a product scan, glues it together with product and client, and saves it all.
     *
     * @param client         Client
     * @param product        Product
     * @param latitude       Optional latitude
     * @param longitude      Optional longitude
     * @param modifyValues   Function called with modified client and product
     * @return               Product scan
     */
    @ClientValid
    open fun createProductScan(client: Client, product: Product, latitude: Double?, longitude: Double?,
                               modifyValues: (Client, Product) -> Unit = { client, product -> Unit}): ProductScan {

        // Create location, if any
        val location = if (latitude != null && longitude != null) Location(latitude, longitude) else null
        if (location != null) {
            locationRepository.save(location)
        }

        // Create product scan
        val productScan = ProductScan()
        productScan.uuid = CryptoUtil().uuid()
        productScan.product = product
        productScan.client = client
        productScan.location = location

        // Glue together with client and product
        client.productScans.add(productScan)
        product.productScans.add(productScan)

        // Save if all
        val modifiedProductScan = productScanRepository.save(productScan)
        val modifiedProduct = productRepository.save(product)
        val modifiedClient = clientRepository.save(client)

        modifyValues(modifiedClient, modifiedProduct)

        return modifiedProductScan
    }

    /**
     * Trust vote product.
     *
     * @param client        Client
     * @param product       Product
     * @param vote          Vote
     * @param modifyValues  Function called with modified client
     * @return              Trust vote
     */
    @ClientVerified
    open fun trustVoteProduct(client: Client, product: Product, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        return trustService.trustVoteItem(client, productTrustItem(product), vote,
                modifyValues = {modifiedClient, trustItem -> modifyValues(modifiedClient)})
    }

    /**
     * Returns the trust item of the given product.
     *
     * @param product   Product
     * @return          Trust item
     */
    open fun productTrustItem(product: Product): TrustItem {
        return trustItemRepository.findByUuid(product.trustItemUuid)!!
    }
}

data class ProductScanResult(
        val productScan: ProductScan,
        val product: Product,
        val recommendations: List<Recommendation>)

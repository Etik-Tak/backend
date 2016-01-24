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
import dk.etiktak.backend.model.contribution.*
import dk.etiktak.backend.model.location.Location
import dk.etiktak.backend.model.product.*
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.contribution.ProductCategoryContributionRepository
import dk.etiktak.backend.repository.contribution.ProductCompanyContributionRepository
import dk.etiktak.backend.repository.contribution.ProductLabelContributionRepository
import dk.etiktak.backend.repository.contribution.ProductNameContributionRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.*
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.recommendation.RecommendationService
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

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
        private val productNameContributionRepository: ProductNameContributionRepository,
        private val productCategoryContributionRepository: ProductCategoryContributionRepository,
        private val productLabelContributionRepository: ProductLabelContributionRepository,
        private val productCompanyContributionRepository: ProductCompanyContributionRepository,
        private val companyRepository: CompanyRepository,
        private val contributionService: ContributionService) {

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
     * @param inClient      Client
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
    open fun createProduct(inClient: Client, barcode: String, barcodeType: Product.BarcodeType?, name: String,
                           categories: List<ProductCategory> = listOf(),
                           labels: List<ProductLabel> = listOf(),
                           companies: List<Company> = listOf(),
                           modifyValues: (Client, List<ProductCategory>, List<ProductLabel>, List<Company>) -> Unit = { client, productCategories, productLabels, companies -> Unit}): Product {

        var client = inClient

        // Create product
        var product = Product()
        product.uuid = CryptoUtil().uuid()
        product.barcode = barcode
        product.barcodeType = barcodeType ?: Product.BarcodeType.UNKNOWN

        product = productRepository.save(product)

        // Create name contribution
        editProductName(client, product, name,
                modifyValues = {modifiedClient, modifiedProduct -> client = modifiedClient; product = modifiedProduct})

        // Assign categories
        var modifiedCategories: MutableList<ProductCategory> = arrayListOf()
        for (productCategory in categories) {
            assignCategoryToProduct(client, product, productCategory,
                    modifyValues = {modifiedClient, modifiedProduct, modifiedCategory -> client = modifiedClient; product = modifiedProduct; modifiedCategories.add(modifiedCategory) })
        }

        // Assign labels
        var modifiedLabels: MutableList<ProductLabel> = arrayListOf()
        for (productLabel in labels) {
            assignLabelToProduct(client, product, productLabel,
                    modifyValues = { modifiedClient, modifiedProduct, modifiedLabel -> client = modifiedClient; product = modifiedProduct; modifiedLabels.add(modifiedLabel)})
        }

        // Assign companies
        var modifiedCompanies: MutableList<Company> = arrayListOf()
        for (company in companies) {
            assignCompanyToProduct(client, product, company,
                    modifyValues = {modifiedClient, modifiedProduct, modifiedCompany -> client = modifiedClient; product = modifiedProduct; modifiedCompanies.add(modifiedCompany)})
        }

        modifyValues(client, modifiedCategories, modifiedLabels, modifiedCompanies)

        return product
    }

    /**
     * Creates a new empty product.
     *
     * @param inClient      Client
     * @param barcode       Barcode
     * @param barcodeType   Barcode type
     * @param modifyValues  Function called with modified client
     * @return              Created product
     */
    open fun createEmptyProduct(inClient: Client, barcode: String, barcodeType: Product.BarcodeType?,
                                modifyValues: (Client) -> Unit = {}): Product {

        var client = inClient

        // Create product
        var product = Product()
        product.uuid = CryptoUtil().uuid()
        product.barcode = barcode
        product.barcodeType = barcodeType ?: Product.BarcodeType.UNKNOWN

        product = productRepository.save(product)

        // Create name contribution
        editProductName(client, product, "",
                modifyValues = {modifiedClient, modifiedProduct -> client = modifiedClient; product = modifiedProduct})

        modifyValues(client)

        return product
    }

    /**
     * Edits a product name.
     *
     * @param inClient      Client
     * @param inProduct     Product
     * @param name          Name of product
     * @param modifyValues  Function called with modified client and product
     * @return              Product name contribution
     */
    @ClientVerified
    open fun editProductName(inClient: Client, inProduct: Product, name: String?, modifyValues: (Client, Product) -> Unit = { client, product -> Unit}): ProductNameContribution {

        var client = inClient
        var product = inProduct

        // Get current name contribution
        val contributions = productNameContributionRepository.findByProductUuidAndEnabled(product.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Check sufficient trust
            contributionService.assertSufficientTrustToEditContribution(client, currentContribution)

            // Disable current contribution
            currentContribution.enabled = false
            productNameContributionRepository.save(currentContribution)
        }

        // Create name contribution
        var productNameContribution = ProductNameContribution()
        productNameContribution.uuid = CryptoUtil().uuid()
        productNameContribution.client = client
        productNameContribution.name = name

        // Glue it together
        product.contributions.add(productNameContribution)
        client.contributions.add(productNameContribution)

        // Save it all
        client = clientRepository.save(client)
        product = productRepository.save(product)
        productNameContribution = productNameContributionRepository.save(productNameContribution)

        // Update trust
        contributionService.updateTrust(productNameContribution)

        modifyValues(client, product)

        return productNameContribution
    }

    /**
     * Assigns a category to a product.
     *
     * @param inClient            Client
     * @param inProduct           Product
     * @param inProductCategory   Product category
     * @param modifyValues        Function called with modified client, product and product category
     * @return                    Product category contribution
     */
    @ClientVerified
    open fun assignCategoryToProduct(inClient: Client, inProduct: Product, inProductCategory: ProductCategory, modifyValues: (Client, Product, ProductCategory) -> Unit = { client, product, productCategory -> Unit}): ProductCategoryContribution {

        var client = inClient
        var product = inProduct
        var productCategory = inProductCategory

        // Get current category contribution
        val contributions = productCategoryContributionRepository.findByProductUuidAndCategoryUuidAndEnabled(product.uuid, productCategory.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Make sure it's disabled
            Assert.isTrue(
                    !currentContribution.enabled,
                    "Cannot assign product category with UUID ${productCategory.uuid} to product with UUID ${product.uuid}; it's already there and enabled!"
            )
        }

        // Create category contribution
        var productCategoryContribution = ProductCategoryContribution()
        productCategoryContribution.uuid = CryptoUtil().uuid()
        productCategoryContribution.client = client
        productCategoryContribution.productCategory = productCategory

        // Glue it together
        productCategory.contributions.add(productCategoryContribution)
        product.contributions.add(productCategoryContribution)
        client.contributions.add(productCategoryContribution)

        // Save it all
        productCategory = productCategoryRepository.save(productCategory)
        client = clientRepository.save(client)
        product = productRepository.save(product)
        productCategoryContribution = productCategoryContributionRepository.save(productCategoryContribution)

        // Update trust
        contributionService.updateTrust(productCategoryContribution)

        modifyValues(client, product, productCategory)

        return productCategoryContribution
    }

    /**
     * Assigns a category to a product.
     *
     * @param inClient            Client
     * @param inProduct           Product
     * @param inProductLabel      Product category
     * @param modifyValues        Function called with modified client, product and product label
     * @return                    Product label contribution
     */
    @ClientVerified
    open fun assignLabelToProduct(inClient: Client, inProduct: Product, inProductLabel: ProductLabel, modifyValues: (Client, Product, ProductLabel) -> Unit = { client, product, productLabel -> Unit}): ProductLabelContribution {

        var client = inClient
        var product = inProduct
        var productLabel = inProductLabel

        // Get current label contribution
        val contributions = productLabelContributionRepository.findByProductUuidAndLabelUuidAndEnabled(product.uuid, productLabel.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Make sure it's disabled
            Assert.isTrue(
                    !currentContribution.enabled,
                    "Cannot assign product label with UUID ${currentContribution.uuid} to product with UUID ${product.uuid}; it's already there and enabled!"
            )
        }

        // Create label contribution
        var productLabelContribution = ProductLabelContribution()
        productLabelContribution.uuid = CryptoUtil().uuid()
        productLabelContribution.client = client
        productLabelContribution.productLabel = productLabel

        // Glue it together
        productLabel.contributions.add(productLabelContribution)
        product.contributions.add(productLabelContribution)
        client.contributions.add(productLabelContribution)

        // Save it all
        productLabel = productLabelRepository.save(productLabel)
        client = clientRepository.save(client)
        product = productRepository.save(product)
        productLabelContribution = productLabelContributionRepository.save(productLabelContribution)

        // Update trust
        contributionService.updateTrust(productLabelContribution)

        modifyValues(client, product, productLabel)

        return productLabelContribution
    }


    /**
     * Assigns a company to a product.
     *
     * @param inClient            Client
     * @param inProduct           Product
     * @param inCompany           Company
     * @param modifyValues        Function called with modified client, product and company
     * @return                    Product company contribution
     */
    @ClientVerified
    open fun assignCompanyToProduct(inClient: Client, inProduct: Product, inCompany: Company, modifyValues: (Client, Product, Company) -> Unit = { client, product, company -> Unit}): ProductCompanyContribution {

        var client = inClient
        var product = inProduct
        var company = inCompany

        // Get current company contribution
        val contributions = productCompanyContributionRepository.findByProductUuidAndEnabled(product.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Check sufficient trust
            contributionService.assertSufficientTrustToEditContribution(client, currentContribution)

            // Disable current contribution
            currentContribution.enabled = false
            productCompanyContributionRepository.save(currentContribution)
        }

        // Create company contribution
        var productCompanyContribution = ProductCompanyContribution()
        productCompanyContribution.uuid = CryptoUtil().uuid()
        productCompanyContribution.client = client
        productCompanyContribution.company = company

        // Glue it together
        company.productContributions.add(productCompanyContribution)
        product.contributions.add(productCompanyContribution)
        client.contributions.add(productCompanyContribution)

        // Save it all
        company = companyRepository.save(company)
        client = clientRepository.save(client)
        product = productRepository.save(product)
        productCompanyContribution = productCompanyContributionRepository.save(productCompanyContribution)

        // Update trust
        contributionService.updateTrust(productCompanyContribution)

        modifyValues(client, product, company)

        return productCompanyContribution
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
     * Returns the product name contribution which is currently active.
     *
     * @param product   Product
     * @return          Product name contribution
     */
    open fun productNameContribution(product: Product): ProductNameContribution? {
        val contributions = productNameContributionRepository.findByProductUuidAndEnabled(product.uuid)
        return contributionService.uniqueContribution(contributions)
    }

    /**
     * Returns the name of a product.
     *
     * @param product   Product
     * @return          Name of product
     */
    open fun productName(product: Product): String? {
        return productNameContribution(product)?.name
    }

    /**
     * Returns whether the given client can edit the name of the product.
     *
     * @param client    Client
     * @param product   Product
     * @return          Yes,if the given client can edit the name of the product, or else false
     */
    open fun canEditProductName(client: Client, product: Product): Boolean {
        return contributionService.hasSufficientTrustToEditContribution(client, productNameContribution(product))
    }

    /**
     * Trust votes product name.
     *
     * @param client          Client
     * @param product         Product
     * @param vote            Vote
     * @param modifyValues    Function called with modified client
     * @return                Trust vote
     */
    open fun trustVoteProductName(client: Client, product: Product, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = productNameContribution(product)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }





    /**
     * Returns the categories of a product.
     *
     * @param product   Product
     * @return          Product categories
     */
    open fun productCategories(product: Product): List<ProductCategory> {
        val contributions = productCategoryContributionRepository.findByProductUuidAndEnabled(product.uuid)
        return contributions.map { contribution -> contribution.productCategory }
    }

    /**
     * Returns the labels of a product.
     *
     * @param product   Product
     * @return          Product labels
     */
    open fun productLabels(product: Product): List<ProductLabel> {
        val contributions = productLabelContributionRepository.findByProductUuidAndEnabled(product.uuid)
        return contributions.map { contribution -> contribution.productLabel }
    }

    /**
     * Returns the company of a product.
     *
     * @param product   Product
     * @return          Company
     */
    open fun productCompany(product: Product): Company? {
        val contributions = productCompanyContributionRepository.findByProductUuidAndEnabled(product.uuid)
        return contributionService.uniqueContribution(contributions)?.company
    }
}

data class ProductScanResult(
        val productScan: ProductScan,
        val product: Product,
        val recommendations: List<Recommendation>)

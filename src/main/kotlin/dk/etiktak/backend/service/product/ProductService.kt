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
        private val productTagRepository: ProductTagRepository,
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
     * @param tags          Product tags
     * @param companies     Companies
     * @param modifyValues  Function called with modified client, product categories, product labels, product tags and companies
     * @return              Created product
     */
    @ClientVerified
    open fun createProduct(inClient: Client, barcode: String, barcodeType: Product.BarcodeType?, name: String,
                           categories: List<ProductCategory> = listOf(),
                           labels: List<ProductLabel> = listOf(),
                           tags: List<ProductTag> = listOf(),
                           companies: List<Company> = listOf(),
                           modifyValues: (Client, List<ProductCategory>, List<ProductLabel>, List<ProductTag>, List<Company>) -> Unit = { client, productCategories, productLabels, productTags, companies -> Unit}): Product {

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

        // Assign tags
        var modifiedTags: MutableList<ProductTag> = arrayListOf()
        for (productTag in tags) {
            assignTagToProduct(client, product, productTag,
                    modifyValues = { modifiedClient, modifiedProduct, modifiedTag -> client = modifiedClient; product = modifiedProduct; modifiedTags.add(modifiedTag)})
        }

        // Assign companies
        var modifiedCompanies: MutableList<Company> = arrayListOf()
        for (company in companies) {
            assignCompanyToProduct(client, product, company,
                    modifyValues = {modifiedClient, modifiedProduct, modifiedCompany -> client = modifiedClient; product = modifiedProduct; modifiedCompanies.add(modifiedCompany)})
        }

        modifyValues(client, modifiedCategories, modifiedLabels, modifiedTags, modifiedCompanies)

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
        editProductName(client, product, "", modifyValues = {modifiedClient, modifiedProduct -> client = modifiedClient; product = modifiedProduct})

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
    open fun editProductName(inClient: Client, inProduct: Product, name: String, modifyValues: (Client, Product) -> Unit = {client, product -> Unit }): Contribution {

        var client = inClient
        var product = inProduct

        // Create contribution
        val contribution = contributionService.createTextContribution(Contribution.ContributionType.ProductName, client, product.uuid, name, modifyValues = {modifiedClient -> client = modifiedClient})

        // Edit name
        product.name = name
        product = productRepository.save(product)

        modifyValues(client, product)

        return contribution
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
    open fun assignCategoryToProduct(inClient: Client, inProduct: Product, inProductCategory: ProductCategory, modifyValues: (Client, Product, ProductCategory) -> Unit = { client, product, productCategory -> Unit}): Contribution {

        var client = inClient
        var product = inProduct
        var productCategory = inProductCategory

        // Create contribution
        val contribution = contributionService.createReferenceContribution(Contribution.ContributionType.ProductCategory, client, product.uuid, productCategory.uuid, modifyValues = { modifiedClient -> client = modifiedClient})

        // Assign category
        product.productCategories.add(productCategory)
        productCategory.products.add(product)

        // Save it all
        productCategory = productCategoryRepository.save(productCategory)
        client = clientRepository.save(client)
        product = productRepository.save(product)

        modifyValues(client, product, productCategory)

        return contribution
    }

    /**
     * Assigns a label to a product.
     *
     * @param inClient            Client
     * @param inProduct           Product
     * @param inProductLabel      Product label
     * @param modifyValues        Function called with modified client, product and product label
     * @return                    Product label contribution
     */
    @ClientVerified
    open fun assignLabelToProduct(inClient: Client, inProduct: Product, inProductLabel: ProductLabel, modifyValues: (Client, Product, ProductLabel) -> Unit = { client, product, productLabel -> Unit}): Contribution {

        var client = inClient
        var product = inProduct
        var productLabel = inProductLabel

        // Create contribution
        val contribution = contributionService.createReferenceContribution(Contribution.ContributionType.ProductLabel, client, product.uuid, productLabel.uuid, modifyValues = { modifiedClient -> client = modifiedClient})

        // Assign label
        product.productLabels.add(productLabel)
        productLabel.products.add(product)

        // Save it all
        productLabel = productLabelRepository.save(productLabel)
        client = clientRepository.save(client)
        product = productRepository.save(product)

        modifyValues(client, product, productLabel)

        return contribution
    }

    /**
     * Assigns a tag to a product.
     *
     * @param inClient            Client
     * @param inProduct           Product
     * @param inProductTag        Product tag
     * @param modifyValues        Function called with modified client, product and product tag
     * @return                    Product tag contribution
     */
    @ClientVerified
    open fun assignTagToProduct(inClient: Client, inProduct: Product, inProductTag: ProductTag, modifyValues: (Client, Product, ProductTag) -> Unit = { client, product, productTag -> Unit}): Contribution {

        var client = inClient
        var product = inProduct
        var productTag = inProductTag

        // Create contribution
        val contribution = contributionService.createReferenceContribution(Contribution.ContributionType.ProductTag, client, product.uuid, productTag.uuid, modifyValues = { modifiedClient -> client = modifiedClient})

        // Assign tag
        product.productTags.add(productTag)
        productTag.products.add(product)

        // Save it all
        productTag = productTagRepository.save(productTag)
        client = clientRepository.save(client)
        product = productRepository.save(product)

        modifyValues(client, product, productTag)

        return contribution
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
    open fun assignCompanyToProduct(inClient: Client, inProduct: Product, inCompany: Company, modifyValues: (Client, Product, Company) -> Unit = { client, product, company -> Unit}): Contribution {

        var client = inClient
        var product = inProduct
        var company = inCompany

        // Create contribution
        val contribution = contributionService.createReferenceContribution(Contribution.ContributionType.ProductCompany, client, product.uuid, company.uuid, modifyValues = { modifiedClient -> client = modifiedClient})

        // Assign company
        product.companies.add(company)
        company.products.add(product)

        // Save it all
        company = companyRepository.save(company)
        client = clientRepository.save(client)
        product = productRepository.save(product)

        modifyValues(client, product, company)

        return contribution
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
    open fun productNameContribution(product: Product): TextContribution? {
        return contributionService.currentTextContribution(Contribution.ContributionType.ProductName, product.uuid)
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
}

data class ProductScanResult(
        val productScan: ProductScan,
        val product: Product,
        val recommendations: List<Recommendation>)

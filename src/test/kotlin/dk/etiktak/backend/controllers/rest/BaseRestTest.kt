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

package dk.etiktak.backend.controllers.rest

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelClientRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceUrlPrefixRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.product.ProductLabelRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.product.ProductScanRepository
import dk.etiktak.backend.repository.recommendation.ProductCategoryRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductLabelRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductRecommendationRepository
import dk.etiktak.backend.repository.recommendation.RecommendationRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.infosource.InfoSourceReferenceService
import dk.etiktak.backend.service.infosource.InfoSourceService
import dk.etiktak.backend.service.product.ProductCategoryService
import dk.etiktak.backend.service.product.ProductLabelService
import dk.etiktak.backend.service.product.ProductService
import dk.etiktak.backend.util.CryptoUtil
import dk.etiktak.backend.util.getWithScale
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.Charset

import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import java.util.*

open class BaseRestTest {

    fun serviceEndpoint(): String {
        return "/service/"
    }



    var product1: Product = Product()
    var product2: Product = Product()

    var productCategory1: ProductCategory = ProductCategory()
    var productCategory2: ProductCategory = ProductCategory()

    var productLabel1: ProductLabel = ProductLabel()
    var productLabel2: ProductLabel = ProductLabel()

    var client1: Client = Client()
    var client2: Client = Client()

    var infoSource1: InfoSource = InfoSource()
    var infoSource2: InfoSource = InfoSource()

    var infoSourceReference1: InfoSourceReference = InfoSourceReference()
    var infoSourceReference2: InfoSourceReference = InfoSourceReference()

    var infoChannel1: InfoChannel = InfoChannel()
    var infoChannel2: InfoChannel = InfoChannel()

    var location1: Location = Location()
    var location2: Location = Location()

    var smsChallenge: String = ""



    @Autowired
    val productScanRepository: ProductScanRepository? = null

    @Autowired
    val productCategoryRepository: ProductCategoryRepository? = null

    @Autowired
    val productLabelRepository: ProductLabelRepository? = null

    @Autowired
    val productRepository: ProductRepository? = null

    @Autowired
    val clientRepository: ClientRepository? = null

    @Autowired
    val locationRepository: LocationRepository? = null

    @Autowired
    val mobileNumberRepository: MobileNumberRepository? = null

    @Autowired
    val smsVerificationRepository: SmsVerificationRepository? = null

    @Autowired
    val infoChannelRepository: InfoChannelRepository? = null

    @Autowired
    val infoChannelClientRepository: InfoChannelClientRepository? = null

    @Autowired
    val infoSourceRepository: InfoSourceRepository? = null

    @Autowired
    val infoSourceUrlPrefixRepository: InfoSourceUrlPrefixRepository? = null

    @Autowired
    val infoSourceReferenceRepository: InfoSourceReferenceRepository? = null

    @Autowired
    val productService: ProductService? = null

    @Autowired
    val productCategoryService: ProductCategoryService? = null

    @Autowired
    val productLabelService: ProductLabelService? = null

    @Autowired
    val infoChannelService: InfoChannelService? = null

    @Autowired
    val infoSourceService: InfoSourceService? = null

    @Autowired
    val infoSourceReferenceService: InfoSourceReferenceService? = null

    @Autowired
    val recommendationRepository: RecommendationRepository? = null

    @Autowired
    val productRecommendationRepository: ProductRecommendationRepository? = null

    @Autowired
    val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository? = null

    @Autowired
    val productLabelRecommendationRepository: ProductLabelRecommendationRepository? = null

    @Autowired
    val clientService: ClientService? = null

    @get:Rule
    public val exception = ExpectedException.none()



    @Throws(Exception::class)
    open fun setup() {
        mockMvcVar = webAppContextSetup(webApplicationContext).build()

        cleanRepository()
    }

    @After
    fun tearDown() {
        cleanRepository()
    }



    fun cleanRepository() {
        recommendationRepository!!.deleteAll()
        productRecommendationRepository!!.deleteAll()
        productCategoryRecommendationRepository!!.deleteAll()
        productLabelRecommendationRepository!!.deleteAll()

        infoSourceUrlPrefixRepository!!.deleteAll()
        infoSourceReferenceRepository!!.deleteAll()
        infoSourceRepository!!.deleteAll()

        infoChannelClientRepository!!.deleteAll()
        infoChannelRepository!!.deleteAll()

        productScanRepository!!.deleteAll()
        locationRepository!!.deleteAll()
        productRepository!!.deleteAll()
        productCategoryRepository!!.deleteAll()
        productLabelRepository!!.deleteAll()

        clientRepository!!.deleteAll()

        mobileNumberRepository!!.deleteAll()
        smsVerificationRepository!!.deleteAll()
    }

    fun createAndSaveProduct(creator: Client, barcode: String, barcodeType: Product.BarcodeType): Product {
        return productService!!.createProduct(creator, barcode, barcodeType, CryptoUtil().uuid(), ArrayList())
    }

    fun createAndSaveProductCategory(creator: Client, product: Product? = null, modifyValues: (Product) -> Unit = {}): ProductCategory {
        val productCategory = productCategoryService!!.createProductCategory(creator, CryptoUtil().uuid(), {})
        product?.let {
            productService!!.assignCategoryToProduct(product.creator, product, productCategory, {product, productCategory -> modifyValues(product)})
        }
        return productCategory
    }

    fun createAndSaveProductLabel(creator: Client, product: Product? = null, modifyValues: (Product) -> Unit = {}): ProductLabel {
        val productLabel = productLabelService!!.createProductLabel(creator, CryptoUtil().uuid())
        product?.let {
            productService!!.assignLabelToProduct(product.creator, product, productLabel, {product, productLabel -> modifyValues(product)})
        }
        return productLabel
    }

    fun createAndSaveInfoChannel(client: Client): InfoChannel {
        return infoChannelService!!.createInfoChannel(client, CryptoUtil().uuid())
    }

    fun createAndSaveInfoSource(client: Client, urlPrefixes: List<String>): InfoSource {
        return infoSourceService!!.createInfoSource(client, urlPrefixes, CryptoUtil().uuid())
    }

    fun createAndSaveInfoSourceReference(client: Client, infoChannel: InfoChannel, infoSource: InfoSource, url: String): InfoSourceReference {
        return infoSourceReferenceService!!.createInfoSourceReference(
                client,
                infoChannel,
                infoSource,
                url,
                "Some title",
                "Some summary.")

    }

    fun createAndSaveClient(): Client {
        return clientService!!.createClient();
    }

    fun createAndSaveLocation(): Location {
        val location = Location()
        location.latitude = Math.random().getWithScale(6)
        location.longitude = Math.random().getWithScale(6)
        locationRepository!!.save(location)
        return location
    }



    @Autowired
    private val webApplicationContext: WebApplicationContext? = null

    @Autowired
    internal fun setConverters(converters: Array<HttpMessageConverter<Any>>) {
        this.mappingJackson2HttpMessageConverter = converters.asList().filter(
                { hmc -> hmc is MappingJackson2HttpMessageConverter }).first()

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter)
    }

    protected var jsonContentType = MediaType(
            MediaType.APPLICATION_JSON.type,
            MediaType.APPLICATION_JSON.subtype,
            Charset.forName("utf8"))

    protected var mockMvcVar: MockMvc? = null

    protected var mappingJackson2HttpMessageConverter: HttpMessageConverter<Any>? = null

    fun mockMvc(): MockMvc {
        return mockMvcVar!!
    }
}
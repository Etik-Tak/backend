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
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelClientRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.product.ProductScanRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import dk.etiktak.backend.service.infosource.InfoSourceService
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

open class BaseRestTest {



    fun serviceEndpoint(): String {
        return "/service/"
    }



    var product1: Product = Product()
    var product2: Product = Product()

    var productCategory1: ProductCategory = ProductCategory()
    var productCategory2: ProductCategory = ProductCategory()

    var client1: Client = Client()
    var client2: Client = Client()

    var infoSource1: InfoSource = InfoSource()
    var infoSource2: InfoSource = InfoSource()

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
    val infoSourceReferenceRepository: InfoSourceReferenceRepository? = null

    @Autowired
    val infoSourceService: InfoSourceService? = null

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
        infoSourceReferenceRepository!!.deleteAll()
        infoSourceRepository!!.deleteAll()

        infoChannelClientRepository!!.deleteAll()
        infoChannelRepository!!.deleteAll()

        productScanRepository!!.deleteAll()
        locationRepository!!.deleteAll()
        productRepository!!.deleteAll()
        productCategoryRepository!!.deleteAll()

        clientRepository!!.deleteAll()

        mobileNumberRepository!!.deleteAll()
        smsVerificationRepository!!.deleteAll()
    }

    fun createAndSaveProduct(creator: Client, barcode: String, barcodeType: Product.BarcodeType): Product {
        val product = Product()
        product.uuid = CryptoUtil().uuid()
        product.creator = creator
        product.name = CryptoUtil().uuid()
        product.barcode = barcode
        product.barcodeType = barcodeType

        creator.products.add(product)

        clientRepository!!.save(creator)
        productRepository!!.save(product)

        return product
    }

    fun createAndSaveProductCategory(creator: Client): ProductCategory {
        val productCategory = ProductCategory()
        productCategory.uuid = CryptoUtil().uuid()
        productCategory.creator = creator
        productCategory.name = CryptoUtil().uuid()

        creator.productCategories.add(productCategory)

        clientRepository!!.save(creator)
        productCategoryRepository!!.save(productCategory)

        return productCategory
    }

    fun createAndSaveInfoChannel(): InfoChannel {
        val infoChannel = InfoChannel()
        infoChannel.uuid = CryptoUtil().uuid()
        infoChannel.name = CryptoUtil().uuid()
        infoChannelRepository!!.save(infoChannel)
        return infoChannel
    }

    fun createAndSaveInfoSource(client: Client, urlPrefixes: List<String>): InfoSource {
        return infoSourceService!!.createInfoSource(client, urlPrefixes, CryptoUtil().uuid())
    }

    fun createAndSaveClient(): Client {
        val client = Client()
        client.uuid = CryptoUtil().uuid()
        client.verified = false
        clientRepository!!.save(client)
        return client
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
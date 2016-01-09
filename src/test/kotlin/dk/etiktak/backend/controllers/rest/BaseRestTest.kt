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

import com.jayway.jsonpath.JsonPath
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.recommendation.RecommendationScore
import dk.etiktak.backend.repository.infochannel.InfoChannelClientRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelFollowerRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceUrlPrefixRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.*
import dk.etiktak.backend.repository.recommendation.ProductCategoryRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductLabelRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductRecommendationRepository
import dk.etiktak.backend.repository.recommendation.RecommendationRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.Charset

import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

data class TestLocation(val latitude: Double, val longitude: Double)

open class BaseRestTest {

    fun serviceEndpoint(): String {
        return "/service/"
    }



    var client1Uuid = ""
    var client2Uuid = ""

    var product1Uuid = ""
    var product2Uuid = ""

    var productCategory1Uuid = ""
    var productCategory2Uuid = ""

    var productLabel1Uuid = ""
    var productLabel2Uuid = ""

    var infoSource1Uuid = ""
    var infoSource2Uuid = ""

    var infoSourceReference1Uuid = ""
    var infoSourceReference2Uuid = ""

    var infoChannel1Uuid = ""
    var infoChannel2Uuid = ""

    var location1: TestLocation = TestLocation(0.0, 0.0)
    var location2: TestLocation = TestLocation(0.0, 0.0)

    var productRecommendation1Uuid = ""
    var productRecommendation2Uuid = ""

    var productCategoryRecommendation1Uuid = ""
    var productCategoryRecommendation2Uuid = ""

    var productLabelRecommendation1Uuid = ""
    var productLabelRecommendation2Uuid = ""

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
    val productTrustVoteRepository: ProductTrustVoteRepository? = null

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
    val infoChannelFollowerRepository: InfoChannelFollowerRepository? = null

    @Autowired
    val infoChannelClientRepository: InfoChannelClientRepository? = null

    @Autowired
    val infoSourceRepository: InfoSourceRepository? = null

    @Autowired
    val infoSourceUrlPrefixRepository: InfoSourceUrlPrefixRepository? = null

    @Autowired
    val infoSourceReferenceRepository: InfoSourceReferenceRepository? = null

    @Autowired
    val recommendationRepository: RecommendationRepository? = null

    @Autowired
    val productRecommendationRepository: ProductRecommendationRepository? = null

    @Autowired
    val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository? = null

    @Autowired
    val productLabelRecommendationRepository: ProductLabelRecommendationRepository? = null

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
        productTrustVoteRepository!!.deleteAll()

        recommendationRepository!!.deleteAll()
        productRecommendationRepository!!.deleteAll()
        productCategoryRecommendationRepository!!.deleteAll()
        productLabelRecommendationRepository!!.deleteAll()

        infoSourceUrlPrefixRepository!!.deleteAll()
        infoSourceReferenceRepository!!.deleteAll()
        infoSourceRepository!!.deleteAll()

        infoChannelFollowerRepository!!.deleteAll()
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

    fun createAndSaveProduct(clientUuid: String, barcode: String, barcodeType: Product.BarcodeType, name: String = "Test product"): String {
        return postAndExtract(ProductServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "name" to name,
                        "barcode" to barcode,
                        "barcodeType" to barcodeType.name),
                "$.product.uuid")
    }

    fun createAndSaveProductCategory(clientUuid: String, name: String, productUuid: String? = null): String {
        val categoryUuid = postAndExtract(ProductCategoryServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "name" to name),
                "$.productCategory.uuid")

        productUuid?.let {
            postAndExtract(ProductServiceTest().serviceEndpoint("assign/category/"),
                    hashMapOf(
                            "clientUuid" to clientUuid,
                            "productUuid" to productUuid,
                            "categoryUuid" to categoryUuid),
                    "$.message")
        }

        return categoryUuid
    }

    fun createAndSaveProductLabel(clientUuid: String, name: String, productUuid: String? = null): String {
        val labelUuid = postAndExtract(ProductLabelServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "name" to name),
                "$.productLabel.uuid")

        productUuid?.let {
            postAndExtract(ProductServiceTest().serviceEndpoint("assign/label/"),
                    hashMapOf(
                            "clientUuid" to clientUuid,
                            "productUuid" to productUuid,
                            "labelUuid" to labelUuid),
                    "$.message")
        }

        return labelUuid
    }

    fun createAndSaveInfoChannel(clientUuid: String, name: String = "Test info channel"): String {
        return postAndExtract(InfoChannelServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "name" to name),
                "$.infoChannel.uuid")
    }

    fun createAndSaveInfoSource(clientUuid: String, urlPrefixes: List<String>): String {
        var prefixString = ""
        var delimiter = ""
        for (urlPrefix in urlPrefixes) {
            prefixString += delimiter
            prefixString += urlPrefix
            delimiter = ","
        }
        return postAndExtract(InfoSourceServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "friendlyName" to "Test info source",
                        "urlPrefixList" to prefixString),
                "$.infoSource.uuid")
    }

    fun createAndSaveInfoSourceReference(clientUuid: String, infoChannelUuid: String, infoSourceUuid: String, url: String): String {
        return postAndExtract(InfoSourceReferenceServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "infoChannelUuid" to infoChannelUuid,
                        "infoSourceUuid" to infoSourceUuid,
                        "url" to url,
                        "title" to "Some title",
                        "summary" to "Some summary"),
                "$.infoSourceReference.uuid")
    }

    fun createAndSaveProductRecommendation(clientUuid: String, infoChannelUuid: String, productUuid: String): String {
        return postAndExtract(RecommendationServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "infoChannelUuid" to infoChannelUuid,
                        "productUuid" to productUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary"),
                "$.recommendation.uuid")
    }

    fun createAndSaveProductCategoryRecommendation(clientUuid: String, infoChannelUuid: String, productCategoryUuid: String): String {
        return postAndExtract(RecommendationServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "infoChannelUuid" to infoChannelUuid,
                        "productCategoryUuid" to productCategoryUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary"),
                "$.recommendation.uuid")
    }

    fun createAndSaveProductLabelRecommendation(clientUuid: String, infoChannelUuid: String, productLabelUuid: String): String {
        return postAndExtract(RecommendationServiceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "clientUuid" to clientUuid,
                        "infoChannelUuid" to infoChannelUuid,
                        "productLabelUuid" to productLabelUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary"),
                "$.recommendation.uuid")
    }

    fun createAndSaveClient(): String {
        return postAndExtract(ClientServiceTest().serviceEndpoint("create/"),
                hashMapOf(),
                "$.client.uuid")
    }

    fun createLocation(latitude: Double, longitude: Double): TestLocation {
        return TestLocation(latitude, longitude)
    }

    fun postAndExtract(url: String, params: Map<String, String>, jsonKey: String): String {
        return callAndExtract(post(url), params, jsonKey)
    }

    fun getAndExtract(url: String, params: Map<String, String>, jsonKey: String): String {
        return callAndExtract(get(url), params, jsonKey)
    }

    fun callAndExtract(requestBuilder: MockHttpServletRequestBuilder, params: Map<String, String>, jsonKey: String): String {
        for ((key, value) in params) {
            requestBuilder.param(key, value)
        }
        val result = mockMvc().perform(requestBuilder).andReturn()
        val json = result.response.contentAsString
        return JsonPath.read(json, jsonKey)
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
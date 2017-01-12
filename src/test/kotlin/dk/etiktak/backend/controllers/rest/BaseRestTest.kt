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

package dk.etiktak.backend.controllers.rest

import com.jayway.jsonpath.JsonPath
import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.recommendation.RecommendationScore
import dk.etiktak.backend.repository.changelog.ChangeLogRepository
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.contribution.*
import dk.etiktak.backend.repository.infochannel.InfoChannelClientRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelFollowerRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceDomainRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.*
import dk.etiktak.backend.repository.recommendation.*
import dk.etiktak.backend.repository.user.ClientDeviceRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import dk.etiktak.backend.security.TokenService
import dk.etiktak.backend.util.CryptoUtil
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.Charset

data class TestLocation(val latitude: Double, val longitude: Double)

open class BaseRestTest {

    fun serviceEndpoint(): String {
        return "/service/"
    }



    var client1DeviceId = ""
    var client2DeviceId = ""

    var nonVerifiedClient1Uuid = ""
    var nonVerifiedClient2Uuid = ""

    var company1Uuid = ""
    var company2Uuid = ""

    var product1Uuid = ""
    var product2Uuid = ""

    var productCategory1Uuid = ""
    var productCategory2Uuid = ""

    var productLabel1Uuid = ""
    var productLabel2Uuid = ""

    var productTag1Uuid = ""
    var productTag2Uuid = ""

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

    var productTagRecommendation1Uuid = ""
    var productTagRecommendation2Uuid = ""

    var companyRecommendation1Uuid = ""
    var companyRecommendation2Uuid = ""

    var smsChallenge: String = ""



    @Autowired
    val tokenService: TokenService? = null

    @Autowired
    val productScanRepository: ProductScanRepository? = null

    @Autowired
    val productCategoryRepository: ProductCategoryRepository? = null

    @Autowired
    val productLabelRepository: ProductLabelRepository? = null

    @Autowired
    val productRepository: ProductRepository? = null

    @Autowired
    val companyRepository: CompanyRepository? = null

    @Autowired
    val trustVoteRepository: TrustVoteRepository? = null

    @Autowired
    val contributionRepository: ContributionRepository? = null

    @Autowired
    val textContributionRepository: TextContributionRepository? = null

    @Autowired
    val clientRepository: ClientRepository? = null

    @Autowired
    val clientDeviceRepository: ClientDeviceRepository? = null

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
    val infoSourceDomainRepository: InfoSourceDomainRepository? = null

    @Autowired
    val infoSourceReferenceRepository: InfoSourceReferenceRepository? = null

    @Autowired
    val recommendationRepository: RecommendationRepository? = null

    @Autowired
    val changeLogRepository: ChangeLogRepository? = null

    @get:Rule
    val exception: ExpectedException = ExpectedException.none()



    @Throws(Exception::class)
    open fun setup() {
        mockMvcVar = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()

        cleanRepository()
    }

    @After
    fun tearDown() {
        cleanRepository()
    }



    fun cleanRepository() {
        trustVoteRepository!!.deleteAll()
        contributionRepository!!.deleteAll()

        infoSourceDomainRepository!!.deleteAll()
        infoSourceReferenceRepository!!.deleteAll()
        infoSourceRepository!!.deleteAll()

        recommendationRepository!!.deleteAll()

        infoChannelFollowerRepository!!.deleteAll()
        infoChannelClientRepository!!.deleteAll()
        infoChannelRepository!!.deleteAll()

        productScanRepository!!.deleteAll()
        locationRepository!!.deleteAll()

        productRepository!!.deleteAll()
        productCategoryRepository!!.deleteAll()
        productLabelRepository!!.deleteAll()

        companyRepository!!.deleteAll()

        clientDeviceRepository!!.deleteAll()
        clientRepository!!.deleteAll()

        mobileNumberRepository!!.deleteAll()
        smsVerificationRepository!!.deleteAll()

        changeLogRepository!!.deleteAll()
    }

    fun createAndSaveCompany(deviceId: String, name: String = "Test company", productUuid: String? = null): String {
        val companyUuid = postAndExtract(CompanyTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name),
                "$.company.uuid")

        productUuid?.let {
            postAndExtract(ProductTest().serviceEndpoint("assign/company/"),
                    hashMapOf(
                            "X-Auth-deviceId" to deviceId),
                    hashMapOf(
                            "productUuid" to productUuid,
                            "companyUuid" to companyUuid),
                    "$.message")
        }

        return companyUuid
    }

    fun createAndSaveProduct(deviceId: String, barcode: String, barcodeType: Product.BarcodeType, name: String = "Test product"): String {
        return postAndExtract(ProductTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name,
                        "barcode" to barcode,
                        "barcodeType" to barcodeType.name),
                "$.product.uuid")
    }

    fun createAndSaveProductCategory(deviceId: String, name: String, productUuid: String? = null): String {
        val categoryUuid = postAndExtract(ProductCategoryTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name),
                "$.productCategory.uuid")

        productUuid?.let {
            postAndExtract(ProductTest().serviceEndpoint("assign/category/"),
                    hashMapOf(
                            "X-Auth-deviceId" to deviceId),
                    hashMapOf(
                            "productUuid" to productUuid,
                            "categoryUuid" to categoryUuid),
                    "$.message")
        }

        return categoryUuid
    }

    fun createAndSaveProductLabel(deviceId: String, name: String, productUuid: String? = null): String {
        val labelUuid = postAndExtract(ProductLabelTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name),
                "$.productLabel.uuid")

        productUuid?.let {
            postAndExtract(ProductTest().serviceEndpoint("assign/label/"),
                    hashMapOf(
                            "X-Auth-deviceId" to deviceId),
                    hashMapOf(
                            "productUuid" to productUuid,
                            "labelUuid" to labelUuid),
                    "$.message")
        }

        return labelUuid
    }

    fun createAndSaveProductTag(deviceId: String, name: String, productUuid: String? = null): String {
        val tagUuid = postAndExtract(ProductTagTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name),
                "$.productTag.uuid")

        productUuid?.let {
            postAndExtract(ProductTest().serviceEndpoint("assign/tag/"),
                    hashMapOf(
                            "X-Auth-deviceId" to deviceId),
                    hashMapOf(
                            "productUuid" to productUuid,
                            "tagUuid" to tagUuid),
                    "$.message")
        }

        return tagUuid
    }

    fun createAndSaveInfoChannel(deviceId: String, name: String = "Test info channel"): String {
        return postAndExtract(InfoChannelTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to name),
                "$.infoChannel.uuid")
    }

    fun createAndSaveInfoSource(deviceId: String, domains: List<String>): String {
        var domainString = ""
        var delimiter = ""
        for (domain in domains) {
            domainString += delimiter
            domainString += domain
            delimiter = ","
        }
        return postAndExtract(InfoSourceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "name" to "Test info source",
                        "domainList" to domainString),
                "$.infoSource.uuid")
    }

    fun createAndSaveInfoSourceReference(deviceId: String, infoChannelUuid: String, infoSourceUuid: String, url: String): String {
        return postAndExtract(InfoSourceReferenceTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "infoSourceUuid" to infoSourceUuid,
                        "url" to url,
                        "title" to "Some title",
                        "summary" to "Some summary"),
                "$.infoSourceReference.uuid")
    }

    fun createAndSaveProductRecommendation(deviceId: String, infoChannelUuid: String, productUuid: String, urlListString: String = "http://dr.dk/somenews"): String {
        return postAndExtract(RecommendationTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "productUuid" to productUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary",
                        "infoSourceReferenceUrlList" to urlListString),
                "$.recommendation.uuid")
    }

    fun createAndSaveProductCategoryRecommendation(deviceId: String, infoChannelUuid: String, productCategoryUuid: String, urlListString: String = "http://dr.dk/somenews"): String {
        return postAndExtract(RecommendationTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "productCategoryUuid" to productCategoryUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary",
                        "infoSourceReferenceUrlList" to urlListString),
                "$.recommendation.uuid")
    }

    fun createAndSaveProductLabelRecommendation(deviceId: String, infoChannelUuid: String, productLabelUuid: String, urlListString: String = "http://dr.dk/somenews"): String {
        return postAndExtract(RecommendationTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "productLabelUuid" to productLabelUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary",
                        "infoSourceReferenceUrlList" to urlListString),
                "$.recommendation.uuid")
    }

    fun createAndSaveProductTagRecommendation(deviceId: String, infoChannelUuid: String, productTagUuid: String, urlListString: String = "http://dr.dk/somenews"): String {
        return postAndExtract(RecommendationTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "productTagUuid" to productTagUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary",
                        "infoSourceReferenceUrlList" to urlListString),
                "$.recommendation.uuid")
    }

    fun createAndSaveCompanyRecommendation(deviceId: String, infoChannelUuid: String, companyUuid: String, urlListString: String = "http://dr.dk/somenews"): String {
        return postAndExtract(RecommendationTest().serviceEndpoint("create/"),
                hashMapOf(
                        "X-Auth-deviceId" to deviceId),
                hashMapOf(
                        "infoChannelUuid" to infoChannelUuid,
                        "companyUuid" to companyUuid,
                        "score" to RecommendationScore.THUMBS_UP.name,
                        "summary" to "Some summary",
                        "infoSourceReferenceUrlList" to urlListString),
                "$.recommendation.uuid")
    }

    fun createAndSaveClient(verified: Boolean = true): String {
        val deviceId = postAndExtract(ClientTest().serviceEndpoint("create/"),
                hashMapOf(),
                hashMapOf(),
                "$.device.id")

        val client = clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client
        client.verified = verified
        clientRepository!!.save(client)
        return deviceId
    }

    fun createAndSaveClient(username: String, password: String, verified: Boolean = true): String {
        val deviceId = postAndExtract(ClientTest().serviceEndpoint("create/"),
                hashMapOf(),
                hashMapOf("username" to username, "password" to password),
                "$.device.id")

        val client = clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client
        client.verified = verified
        clientRepository!!.save(client)
        return deviceId
    }

    fun createLocation(latitude: Double, longitude: Double): TestLocation {
        return TestLocation(latitude, longitude)
    }



    fun setClientTrustLevel(deviceId: String, trustLevel: Double) {
        val client = clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client
        client.trustLevel = trustLevel
        clientRepository!!.save(client)
    }

    fun clientTrustLevel(deviceId: String): Double {
        return clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client.trustLevel
    }

    fun setProductNameTrustScore(productUuid: String, trustScore: Double) {
        val product = productRepository!!.findByUuid(productUuid)!!
        val contribution = textContributionRepository!!.findBySubjectUuidAndType(product.uuid, Contribution.ContributionType.ProductName,
                PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))[0]
        contribution.trustScore = trustScore
        textContributionRepository.save(contribution)
    }

    fun productNameTrustLevel(productUuid: String): Double {
        val product = productRepository!!.findByUuid(productUuid)!!
        val contribution = textContributionRepository!!.findBySubjectUuidAndType(product.uuid, Contribution.ContributionType.ProductName,
                PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))[0]
        return contribution.trustScore
    }

    fun setProductLabelNameTrustScore(productLabelUuid: String, trustScore: Double) {
        val productLabel = productLabelRepository!!.findByUuid(productLabelUuid)!!
        val contribution = textContributionRepository!!.findBySubjectUuidAndType(productLabel.uuid, Contribution.ContributionType.ProductLabelName,
                PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))[0]
        contribution.trustScore = trustScore
        textContributionRepository.save(contribution)
    }

    fun setProductCategoryNameTrustScore(productCategoryUuid: String, trustScore: Double) {
        val productCategory = productCategoryRepository!!.findByUuid(productCategoryUuid)!!
        val contribution = textContributionRepository!!.findBySubjectUuidAndType(productCategory.uuid, Contribution.ContributionType.ProductCategoryName,
                PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))[0]
        contribution.trustScore = trustScore
        textContributionRepository.save(contribution)
    }

    fun setInfoSourceNameTrustScore(infoSourceUuid: String, trustScore: Double) {
        val infoSource = infoSourceRepository!!.findByUuid(infoSourceUuid)!!
        val contribution = textContributionRepository!!.findBySubjectUuidAndType(infoSource.uuid, Contribution.ContributionType.InfoSourceName,
                PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))[0]
        contribution.trustScore = trustScore
        textContributionRepository.save(contribution)
    }



    fun postAndExtract(url: String, headers: Map<String, String>, params: Map<String, String>, jsonKey: String): String {
        return callAndExtract(post(url), headers, params, jsonKey)
    }

    fun getAndExtract(url: String, headers: Map<String, String>, params: Map<String, String>, jsonKey: String): String {
        return callAndExtract(get(url), headers, params, jsonKey)
    }

    fun callAndExtract(requestBuilder: MockHttpServletRequestBuilder, headers: Map<String, String>, params: Map<String, String>, jsonKey: String): String {
        for ((key, value) in headers) {
            requestBuilder.header(key, value)
        }
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
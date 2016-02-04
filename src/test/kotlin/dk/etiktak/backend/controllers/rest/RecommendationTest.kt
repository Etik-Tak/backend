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

import dk.etiktak.backend.Application
import dk.etiktak.backend.controller.rest.WebserviceResult
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.recommendation.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class RecommendationTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "recommendation/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1Uuid = createAndSaveClient()
        client2Uuid = createAndSaveClient()

        infoChannel1Uuid = createAndSaveInfoChannel(client1Uuid)
        infoChannel2Uuid = createAndSaveInfoChannel(client2Uuid)

        product1Uuid = createAndSaveProduct(client1Uuid, "12345678a", Product.BarcodeType.EAN13)
        product2Uuid = createAndSaveProduct(client2Uuid, "12345678b", Product.BarcodeType.EAN13)

        productCategory1Uuid = createAndSaveProductCategory(client1Uuid, product1Uuid)
        productCategory2Uuid = createAndSaveProductCategory(client2Uuid, product2Uuid)

        productLabel1Uuid = createAndSaveProductLabel(client1Uuid, product1Uuid)
        productLabel2Uuid = createAndSaveProductLabel(client2Uuid, product2Uuid)

        productTag1Uuid = createAndSaveProductTag(client1Uuid, product1Uuid)
        productTag2Uuid = createAndSaveProductTag(client2Uuid, product2Uuid)

        company1Uuid = createAndSaveCompany(client1Uuid)
        company2Uuid = createAndSaveCompany(client2Uuid)
    }

    /**
     * Test that we can create a product recommendation.
     */
    @Test
    fun createProductRecommendation() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test product 1")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productUuid", product1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test product 1")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_UP.name)))

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test product 2")
                        .param("score", RecommendationScore.THUMBS_DOWN.name)
                        .param("productUuid", product2Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test product 2")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_DOWN.name)))

        val recommendations = recommendationRepository!!.findAll()
        Assert.assertEquals(
                2,
                recommendations.collectionSizeOrDefault(0))

        Assert.assertEquals(
                product1Uuid,
                (recommendations.first() as ProductRecommendation).product.uuid)

        Assert.assertEquals(
                product2Uuid,
                (recommendations.last() as ProductRecommendation).product.uuid)
    }

    /**
     * Test that we can create a product category recommendation.
     */
    @Test
    fun createProductCategoryRecommendation() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category 1")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test category 1")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_UP.name)))

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category 2")
                        .param("score", RecommendationScore.THUMBS_DOWN.name)
                        .param("productCategoryUuid", productCategory2Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test category 2")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_DOWN.name)))

        val recommendations = recommendationRepository!!.findAll()
        Assert.assertEquals(
                2,
                recommendations.collectionSizeOrDefault(0))

        Assert.assertEquals(
                productCategory1Uuid,
                (recommendations.first() as ProductCategoryRecommendation).productCategory.uuid)

        Assert.assertEquals(
                productCategory2Uuid,
                (recommendations.last() as ProductCategoryRecommendation).productCategory.uuid)
    }

    /**
     * Test that we can create a product label recommendation.
     */
    @Test
    fun createProductLabelRecommendation() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test label 1")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productLabelUuid", productLabel1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test label 1")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_UP.name)))

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test label 2")
                        .param("score", RecommendationScore.THUMBS_DOWN.name)
                        .param("productLabelUuid", productLabel2Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test label 2")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_DOWN.name)))

        val recommendations = recommendationRepository!!.findAll()
        Assert.assertEquals(
                2,
                recommendations.collectionSizeOrDefault(0))

        Assert.assertEquals(
                productLabel1Uuid,
                (recommendations.first() as ProductLabelRecommendation).productLabel.uuid)

        Assert.assertEquals(
                productLabel2Uuid,
                (recommendations.last() as ProductLabelRecommendation).productLabel.uuid)
    }

    /**
     * Test that we can create a product tag recommendation.
     */
    @Test
    fun createProductTagRecommendation() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test tag 1")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productTagUuid", productTag1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test tag 1")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_UP.name)))

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test tag 2")
                        .param("score", RecommendationScore.THUMBS_DOWN.name)
                        .param("productTagUuid", productTag2Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test tag 2")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_DOWN.name)))

        val recommendations = recommendationRepository!!.findAll()
        Assert.assertEquals(
                2,
                recommendations.collectionSizeOrDefault(0))

        Assert.assertEquals(
                productTag1Uuid,
                (recommendations.first() as ProductTagRecommendation).productTag.uuid)

        Assert.assertEquals(
                productTag2Uuid,
                (recommendations.last() as ProductTagRecommendation).productTag.uuid)
    }

    /**
     * Test that we can create a company recommendation.
     */
    @Test
    fun createCompanyRecommendation() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test company 1")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("companyUuid", company1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test company 1")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_UP.name)))

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test company 2")
                        .param("score", RecommendationScore.THUMBS_DOWN.name)
                        .param("companyUuid", company2Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.recommendation.uuid", notNullValue()))
                .andExpect(jsonPath("$.recommendation.summary", `is`("Test company 2")))
                .andExpect(jsonPath("$.recommendation.score", `is`(RecommendationScore.THUMBS_DOWN.name)))

        val recommendations = recommendationRepository!!.findAll()
        Assert.assertEquals(
                2,
                recommendations.collectionSizeOrDefault(0))

        Assert.assertEquals(
                company1Uuid,
                (recommendations.first() as CompanyRecommendation).company.uuid)

        Assert.assertEquals(
                company2Uuid,
                (recommendations.last() as CompanyRecommendation).company.uuid)
    }

    /**
     * Test that we cannot create several recommendations for same product and info channel.
     */
    @Test
    fun cannotCreateSeveralRecommendationsForSameProductAndInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productUuid", product1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productUuid", product1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
    }

    /**
     * Test that we cannot create several recommendations for same product category and info channel.
     */
    @Test
    fun cannotCreateSeveralRecommendationsForSameProductCategoryAndInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test category")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
    }

    /**
     * Test that we cannot create several recommendations for same product label and info channel.
     */
    @Test
    fun cannotCreateSeveralRecommendationsForSameProductLabelAndInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test label")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productLabelUuid", productLabel1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test label")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productLabelUuid", productLabel1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
    }

    /**
     * Test that we cannot create several recommendations for same product tag and info channel.
     */
    @Test
    fun cannotCreateSeveralRecommendationsForSameProductTagAndInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test tag")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productTagUuid", productTag1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test tag")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("productTagUuid", productTag1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
    }

    /**
     * Test that we cannot create several recommendations for same company and info channel.
     */
    @Test
    fun cannotCreateSeveralRecommendationsForSameCompanyAndInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test company")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("companyUuid", company1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
                .andExpect(status().isOk)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
                        .param("infoChannelUuid", infoChannel1Uuid)
                        .param("summary", "Test company")
                        .param("score", RecommendationScore.THUMBS_UP.name)
                        .param("companyUuid", company1Uuid)
                        .param("infoSourceReferenceUrlList", "http://dr.dk/somenews/"))
    }
}

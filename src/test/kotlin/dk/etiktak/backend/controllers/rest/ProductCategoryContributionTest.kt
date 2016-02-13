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
import dk.etiktak.backend.model.contribution.TrustVote
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductCategoryContributionTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/category/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
        client2DeviceId = createAndSaveClient()

        product1Uuid = createAndSaveProduct(client1DeviceId, "12345678a", Product.BarcodeType.EAN13, "Test product 1")
        product2Uuid = createAndSaveProduct(client2DeviceId, "12345678b", Product.BarcodeType.UPC, "Test product 2")

        productCategory1Uuid = createAndSaveProductCategory(client1DeviceId, "Product category 1", product1Uuid)
        productCategory2Uuid = createAndSaveProductCategory(client1DeviceId, "Product category 2", product1Uuid)
    }

    /**
     * Test that we can edit a product category name if we have sufficient trust level.
     */
    @Test
    fun editProductCategoryNameWhenSufficientTrusLevelt() {
        setClientTrustLevel(client1DeviceId, 0.7)
        setProductCategoryNameTrustScore(productCategory1Uuid, 0.4)

        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("name", "Product category new"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.productCategory.name", `is`("Product category new")))
    }

    /**
     * Test that we cannot edit a product category name if we don't have sufficient trust level.
     */
    @Test
    fun cannotEditProductCategoryNameWhenInsufficientTrustLevel() {
        setClientTrustLevel(client1DeviceId, 0.5)
        setProductCategoryNameTrustScore(productCategory1Uuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("name", "Product category new"))
    }

    /**
     * Test that a client can trust vote a product category name.
     */
    @Test
    fun trustVoteProductCategoryName() {
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that a client cannot trust vote more than once on same product category name.
     */
    @Test
    fun cannotTrustVoteMoreThanOnceOnSameProductCategoryName() {
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
    }

    /**
     * Test that a client cannot vote on product category names he edited himself.
     */
    @Test
    fun cannotTrustVoteOwnProductCategoryName() {
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productCategoryUuid", productCategory1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }
}

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
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductTrustTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1Uuid = createAndSaveClient()
        client2Uuid = createAndSaveClient()

        product1Uuid = createAndSaveProduct(client1Uuid, "12345678a", Product.BarcodeType.EAN13, "Test product 1")
        product2Uuid = createAndSaveProduct(client2Uuid, "12345678b", Product.BarcodeType.UPC, "Test product 2")

        productCategory1Uuid = createAndSaveProductCategory(client1Uuid, "Product category 1", product1Uuid)
        productCategory2Uuid = createAndSaveProductCategory(client1Uuid, "Product category 2", product1Uuid)

        productLabel1Uuid = createAndSaveProductLabel(client1Uuid, "Product label 1", product1Uuid)
        productLabel2Uuid = createAndSaveProductLabel(client1Uuid, "Product label 2", product1Uuid)
    }

    /**
     * Test that we can create a product if we have sufficient trust level.
     */
    @Test
    fun createProductWhenSufficientTrustLevel() {
        setClientTrustLevel(client1Uuid, 0.5)

        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .param("clientUuid", client1Uuid)
                        .param("name", "Coca Cola")
                        .param("barcode", "12345678")
                        .param("barcodeType", "${Product.BarcodeType.EAN13.name}")
                        .param("categoryUuidList", "${productCategory1Uuid}, ${productCategory2Uuid}")
                        .param("labelUuidList", "${productLabel1Uuid}, ${productLabel2Uuid}"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Coca Cola")))
    }

    /**
     * Test that we can edit a product if we have sufficient trust level.
     */
    @Test
    fun editProductWhenSufficientTrusLevelt() {
        setClientTrustLevel(client1Uuid, 0.7)
        setProductCorrectnessTrust(product1Uuid, 0.4)

        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .param("clientUuid", client1Uuid)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Pepsi Cola")))
    }

    /**
     * Test that we cannot edit a product if we don't have sufficient trust level.
     */
    @Test
    fun cannotEditProductWhenInsufficientTrustLevel() {
        setClientTrustLevel(client1Uuid, 0.5)
        setProductCorrectnessTrust(product1Uuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .param("clientUuid", client1Uuid)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
    }

    private fun setClientTrustLevel(clientUuid: String, trustLevel: Double) {
        val client = clientRepository!!.findByUuid(clientUuid)
        client!!.trustLevel = trustLevel
        clientRepository.save(client)
    }

    private fun setProductCorrectnessTrust(productUuid: String, correctnessTrust: Double) {
        val product = productRepository!!.findByUuidAndEnabled(productUuid)
        product!!.correctnessTrust = correctnessTrust
        productRepository.save(product)
    }
}

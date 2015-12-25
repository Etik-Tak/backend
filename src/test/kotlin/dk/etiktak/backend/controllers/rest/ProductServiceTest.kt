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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductServiceTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1 = createAndSaveClient()

        product1 = createAndSaveProduct(client1, "123456789a", Product.BarcodeType.EAN13)
        product2 = createAndSaveProduct(client2, "123456789b", Product.BarcodeType.UPC)

        productCategory1 = createAndSaveProductCategory(client1, product1)
        product1 = productService!!.getProductByUuid(product1.uuid)!!

        productCategory2 = createAndSaveProductCategory(client2, product1)
        product1 = productService!!.getProductByUuid(product1.uuid)!!

        productLabel1 = createAndSaveProductLabel(client1, product1)
        product1 = productService!!.getProductByUuid(product1.uuid)!!

        productLabel2 = createAndSaveProductLabel(client2, product1)
        product1 = productService!!.getProductByUuid(product1.uuid)!!
    }

    /**
     * Test that we can retrieve product by UUID.
     */
    @Test
    fun retrieveProductByUuid() {
        mockMvc().perform(
                get(serviceEndpoint(""))
                        .param("uuid", product1.uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.uuid", `is`(product1.uuid)))
                .andExpect(jsonPath("$.product.barcode", `is`(product1.barcode)))
                .andExpect(jsonPath("$.product.barcodeType", `is`(product1.barcodeType.name)))
                .andExpect(jsonPath("$.product.labels", hasSize<Any>(2)))
                .andExpect(jsonPath("$.product.categories", hasSize<Any>(2)))
    }

    /**
     * Test that we can retrieve product by EAN13 barcode.
     */
    @Test
    fun retrieveProductByEan13Barcode() {
        mockMvc().perform(
                get(serviceEndpoint(""))
                        .param("barcode", product1.barcode))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.uuid", `is`(product1.uuid)))
                .andExpect(jsonPath("$.product.barcode", `is`(product1.barcode)))
                .andExpect(jsonPath("$.product.barcodeType", `is`(product1.barcodeType.name)))
    }

    /**
     * Test that we can retrieve product by UPC barcode.
     */
    @Test
    fun retrieveProductByUPCBarcode() {
        mockMvc().perform(
                get(serviceEndpoint("/"))
                        .param("barcode", product2.barcode))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.uuid", `is`(product2.uuid)))
                .andExpect(jsonPath("$.product.barcode", `is`(product2.barcode)))
                .andExpect(jsonPath("$.product.barcodeType", `is`(product2.barcodeType.name)))
    }

    /**
     * Test that we can create a product without barcode and categories.
     */
    @Test
    fun createProductWithoutBarcodeAndCategories() {
        mockMvc().perform(
                get(serviceEndpoint("/create/"))
                        .param("clientUuid", client1.uuid)
                        .param("name", "Coca Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.name", `is`("Coca Cola")))
                .andExpect(jsonPath("$.product.barcode", isEmptyOrNullString()))
    }

    /**
     * Test that we can create a product without barcode.
     */
    @Test
    fun createProductWithoutBarcode() {
        mockMvc().perform(
                get(serviceEndpoint("/create/"))
                        .param("clientUuid", client1.uuid)
                        .param("name", "Coca Cola")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"{$productCategory1.uuid}, ${productCategory2.uuid}\"]"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.name", `is`("Coca Cola")))
                .andExpect(jsonPath("$.product.barcode", isEmptyOrNullString()))
    }
}

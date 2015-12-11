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
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.util.CryptoUtil
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductRetrievalServiceTest : BaseRestTest() {

    @Autowired
    private val productRepository: ProductRepository? = null

    private var product1: Product = Product()
    private var product2: Product = Product()

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/retrieve/" + postfix
    }

    @Before
    @Throws(Exception::class)
    override fun setup() {
        super.setup()

        productRepository!!.deleteAll()

        product1 = createAndSaveProduct("123456789a", Product.BarcodeType.EAN13)
        product2 = createAndSaveProduct("123456789b", Product.BarcodeType.UPC)
    }

    @After
    fun tearDown() {
        productRepository!!.deleteAll()
    }

    /**
     * Test that we can retrieve product by UUID.
     */
    @Test
    @Throws(Exception::class)
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
    }

    /**
     * Test that we can retrieve product by EAN13 barcode.
     */
    @Test
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun retrieveProductByUPCBarcode() {
        mockMvc().perform(
                get(serviceEndpoint(""))
                        .param("barcode", product2.barcode))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.product.uuid", `is`(product2.uuid)))
                .andExpect(jsonPath("$.product.barcode", `is`(product2.barcode)))
                .andExpect(jsonPath("$.product.barcodeType", `is`(product2.barcodeType.name)))
    }


    private fun createAndSaveProduct(barcode: String, barcodeType: Product.BarcodeType): Product {
        val product = Product()
        product.uuid = CryptoUtil().uuid()
        product.barcode = barcode
        product.barcodeType = barcodeType
        productRepository!!.save(product)
        return product
    }
}
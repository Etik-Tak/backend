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
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.model.user.Client

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.util.Assert
import org.springframework.web.util.NestedServletException
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductScanServiceTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/scan/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1 = createAndSaveClient()

        product1 = createAndSaveProduct(client1, "123456789a", Product.BarcodeType.EAN13)
        product2 = createAndSaveProduct(client1, "123456789b", Product.BarcodeType.UPC)

        client1 = createAndSaveClient()
        client2 = createAndSaveClient()

        location1 = createAndSaveLocation()
        location2 = createAndSaveLocation()
    }

    /**
     * Test that we can retrieve a product by scanning with location.
     */
    @Test
    fun scanProductWithLocation() {
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.barcode)
                        .param("clientUuid", client1.uuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.productScan.uuid", notNullValue()))
                .andExpect(jsonPath("$.productScan.product.uuid", `is`(product1.uuid)))
                .andExpect(jsonPath("$.productScan.product.name", `is`(product1.name)))
                .andExpect(jsonPath("$.productScan.product.barcode", `is`(product1.barcode)))
                .andExpect(jsonPath("$.productScan.product.barcodeType", `is`(product1.barcodeType.name)))

        validateProductScan(product1, client1, location1)
    }

    /**
     * Test that we can retrieve a product by scanning without location.
     */
    @Test
    fun scanProductWithoutLocation() {
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.barcode)
                        .param("clientUuid", client1.uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.productScan.uuid", notNullValue()))
                .andExpect(jsonPath("$.productScan.product.uuid", `is`(product1.uuid)))
                .andExpect(jsonPath("$.productScan.product.name", `is`(product1.name)))
                .andExpect(jsonPath("$.productScan.product.barcode", `is`(product1.barcode)))
                .andExpect(jsonPath("$.productScan.product.barcodeType", `is`(product1.barcodeType.name)))

        validateProductScan(product1, client1)
    }

    /**
     * Test that we can assign a location to an already existant product scan.
     */
    @Test
    fun assignLocationToProductScan() {
        val productScan = scanProduct()

        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.uuid)
                        .param("productScanUuid", productScan.uuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.productScan.uuid", `is`(productScan.uuid)))
                .andExpect(jsonPath("$.productScan.location.latitude", `is`(location1.latitude)))
                .andExpect(jsonPath("$.productScan.location.longitude", `is`(location1.longitude)))
    }

    /**
     * Test that we cannot assign a location to a product scan that already has a location assigned.
     */
    @Test
    fun cannotAssignLocationToProductScanWithLocationAlreadyAssigned() {
        val productScan = scanProduct()

        // Assign first location
        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.uuid)
                        .param("productScanUuid", productScan.uuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)

        // Do it once again and fail
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.uuid)
                        .param("productScanUuid", productScan.uuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
    }

    /**
     * Test that we cannot assign empty location to already scanned product scan.
     */
    @Test
    fun cannotAssignEmptyLocationToProductScanWithLocationAlreadyAssigned() {
        val productScan = scanProduct()

        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.uuid)
                        .param("productScanUuid", productScan.uuid))
                .andExpect(status().`is`(400))
    }


    private fun scanProduct(): ProductScan {
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.barcode)
                        .param("clientUuid", client1.uuid))
                .andExpect(jsonPath("$.productScan.uuid", notNullValue()))
                .andExpect(jsonPath("$.productScan.location", nullValue()))

        val productScans = productScanRepository!!.findByProductUuid(product1.uuid)
        return productScans[0]
    }

    private fun validateProductScan(product: Product, client: Client) {
        validateProductScan(product, client, null)
    }

    private fun validateProductScan(product: Product, client: Client, location: Location?) {
        val productScansFromProduct = productScanRepository!!.findByProductUuid(product.uuid)
        Assert.notEmpty(productScansFromProduct, "Did not find product scan for product with uuid: " + product.uuid)
        Assert.isTrue(productScansFromProduct.size == 1, "More than one product scan found for product with uuid: " + product.uuid)
        validateProductScan(productScansFromProduct[0], product, client, location)

        val productScansFromClient = productScanRepository.findByClientUuid(client.uuid)
        Assert.notEmpty(productScansFromClient, "Did not find product scan for client with uuid: " + client.uuid)
        Assert.isTrue(productScansFromClient.size == 1, "More than one product scan found for client with uuid: " + client.uuid)
        validateProductScan(productScansFromClient[0], product, client, location)
    }

    private fun validateProductScan(productScan: ProductScan, product: Product, client: Client, location: Location?) {
        Assert.isTrue(productScan.product.uuid == product.uuid, "Product scan's product was not the product expected!")
        Assert.isTrue(productScan.client.uuid == client.uuid, "Product scan's client was not the client expected!")
        if (location != null && productScan.location != null) {
            Assert.isTrue(productScan.location!!.latitude == location.latitude, "Latitude for product scan not correct")
            Assert.isTrue(productScan.location!!.longitude == location.longitude, "Longitude for product scan not correct")
        } else {
            Assert.isNull(productScan.location, "Location for product scan was expected to be null, but was not!")
        }
    }
}
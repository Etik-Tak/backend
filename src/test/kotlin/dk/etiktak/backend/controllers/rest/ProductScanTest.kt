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
import dk.etiktak.backend.Application
import dk.etiktak.backend.controller.rest.WebserviceResult
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.util.CryptoUtil
import org.hamcrest.Matchers.*

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

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
open class ProductScanTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/scan/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
        client2DeviceId = createAndSaveClient()

        product1Uuid = createAndSaveProduct(client1DeviceId, "12345678a", Product.BarcodeType.EAN13)
        product2Uuid = createAndSaveProduct(client2DeviceId, "12345678b", Product.BarcodeType.UPC)

        productCategory1Uuid = createAndSaveProductCategory(client1DeviceId, "Test category 1", product1Uuid)
        productCategory2Uuid = createAndSaveProductCategory(client2DeviceId, "Test category 2", product2Uuid)

        productLabel1Uuid = createAndSaveProductLabel(client1DeviceId, "Test label 1", product1Uuid)
        productLabel2Uuid = createAndSaveProductLabel(client2DeviceId, "Test label 2", product2Uuid)

        productTag1Uuid = createAndSaveProductTag(client1DeviceId, "Test tag 1", product1Uuid)
        productTag2Uuid = createAndSaveProductTag(client2DeviceId, "Test tag 2", product2Uuid)

        company1Uuid = createAndSaveCompany(client1DeviceId, "Test company 1", product1Uuid)
        company2Uuid = createAndSaveCompany(client2DeviceId, "Test company 2", product2Uuid)

        location1 = TestLocation(56.0, 60.0)
        location2 = TestLocation(56.1, 60.1)

        infoChannel1Uuid = createAndSaveInfoChannel(client1DeviceId)
        infoChannel2Uuid = createAndSaveInfoChannel(client2DeviceId)

        productRecommendation1Uuid = createAndSaveProductRecommendation(client1DeviceId, infoChannel1Uuid, product1Uuid)
        productRecommendation2Uuid = createAndSaveProductRecommendation(client2DeviceId, infoChannel2Uuid, product2Uuid)

        productCategoryRecommendation1Uuid = createAndSaveProductCategoryRecommendation(client1DeviceId, infoChannel1Uuid, productCategory1Uuid)
        productCategoryRecommendation2Uuid = createAndSaveProductCategoryRecommendation(client2DeviceId, infoChannel2Uuid, productCategory2Uuid)

        productLabelRecommendation1Uuid = createAndSaveProductLabelRecommendation(client1DeviceId, infoChannel1Uuid, productLabel1Uuid)
        productLabelRecommendation2Uuid = createAndSaveProductLabelRecommendation(client2DeviceId, infoChannel2Uuid, productLabel2Uuid)

        productTagRecommendation1Uuid = createAndSaveProductTagRecommendation(client1DeviceId, infoChannel1Uuid, productTag1Uuid)
        productTagRecommendation2Uuid = createAndSaveProductTagRecommendation(client2DeviceId, infoChannel2Uuid, productTag2Uuid)

        companyRecommendation1Uuid = createAndSaveCompanyRecommendation(client1DeviceId, infoChannel1Uuid, company1Uuid)
        companyRecommendation2Uuid = createAndSaveCompanyRecommendation(client2DeviceId, infoChannel2Uuid, company2Uuid)
    }

    /**
     * Test that we can retrieve a product by scanning with location.
     */
    @Test
    fun scanProductWithLocation() {
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", "12345678a")
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.scan.uuid", notNullValue()))
                .andExpect(jsonPath("$.scan.product.uuid", `is`(product1Uuid)))
                .andExpect(jsonPath("$.scan.product.name", `is`("Test product")))
                .andExpect(jsonPath("$.scan.product.categories", hasSize<Any>(1)))
                .andExpect(jsonPath("$.scan.product.labels", hasSize<Any>(1)))
                .andExpect(jsonPath("$.scan.product.tags", hasSize<Any>(1)))
                .andExpect(jsonPath("$.scan.recommendations", hasSize<Any>(5)))

        validateProductScan(product1Uuid, client1DeviceId, location1)
    }

    /**
     * Test that we can retrieve a product by scanning without location.
     */
    @Test
    fun scanProductWithoutLocation() {
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", "12345678a")
                        .header("X-Auth-DeviceId", client1DeviceId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.scan.product.uuid", `is`(product1Uuid)))
                .andExpect(jsonPath("$.scan.product.name", `is`("Test product")))

        validateProductScan(product1Uuid, client1DeviceId)
    }

    /**
     * Test that a new product is created when scanning non existing product.
     */
    @Test
    fun scanNonExistingProductWillCreateNewProduct() {
        val json = mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", "product_that_does_not_exist")
                        .header("X-Auth-DeviceId", client1DeviceId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.scan.product.uuid", notNullValue()))
                .andExpect(jsonPath("$.scan.product.name", `is`("")))
                .andExpect(jsonPath("$.scan.product.editableItems.name.trustScore", `is`(0.5)))
                .andReturn().response.contentAsString
        val productUuid = JsonPath.read<String>(json, "$.scan.product.uuid")

        // Now see that the same product is returned when scanning second time
        mockMvc().perform(
                post(serviceEndpoint(""))
                        .param("barcode", "product_that_does_not_exist")
                        .header("X-Auth-DeviceId", client1DeviceId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.scan.product.uuid", `is`(productUuid)))
                .andExpect(jsonPath("$.scan.product.name", `is`("")))
                .andExpect(jsonPath("$.scan.product.editableItems.name.trustScore", `is`(0.5)))
    }

    /**
     * Test that we can assign a location to an already existant product scan.
     */
    @Test
    fun assignLocationToProductScan() {
        val productScanUuid = scanProduct()

        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productScanUuid", productScanUuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.scan.uuid", `is`(productScanUuid)))

        val location = productScanRepository!!.findAll()!!.first()!!.location!!
        Assert.isTrue(
                location.latitude == location1.latitude,
                "Expected latitude ${location1.latitude} but got ${location.latitude}"
        )
        Assert.isTrue(
                location.longitude == location1.longitude,
                "Expected longitude ${location1.longitude} but got ${location.longitude}"
        )
    }

    /**
     * Test that we cannot assign a location to a product scan that already has a location assigned.
     */
    @Test
    fun cannotAssignLocationToProductScanWithLocationAlreadyAssigned() {
        val productScanUuid = scanProduct()

        // Assign first location
        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productScanUuid", productScanUuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
                .andExpect(status().isOk)

        // Do it once again and fail
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productScanUuid", productScanUuid)
                        .param("latitude", "" + location1.latitude)
                        .param("longitude", "" + location1.longitude))
    }

    /**
     * Test that we cannot assign empty location to already scanned product scan.
     */
    @Test
    fun cannotAssignEmptyLocationToProductScanWithLocationAlreadyAssigned() {
        val productScanUuid = scanProduct()

        mockMvc().perform(
                post(serviceEndpoint("assign/location/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productScanUuid", productScanUuid))
                .andExpect(status().`is`(400))
    }



    private fun scanProduct(): String {
        return postAndExtract(serviceEndpoint(""),
                hashMapOf(
                        "X-Auth-DeviceId" to client1DeviceId),
                hashMapOf(
                        "barcode" to "12345678a"),
                "$.scan.uuid")
    }

    private fun validateProductScan(productUuid: String, deviceId: String) {
        validateProductScan(productUuid, deviceId, null)
    }

    private fun validateProductScan(productUuid: String, deviceId: String, location: TestLocation?) {
        val client = clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client

        val productScansFromProduct = productScanRepository!!.findByProductUuid(productUuid)
        Assert.notEmpty(productScansFromProduct, "Did not find product scan for product with uuid: " + productUuid)
        Assert.isTrue(productScansFromProduct.size == 1, "More than one product scan found for product with uuid: " + productUuid)
        validateProductScan(productScansFromProduct[0], productUuid, deviceId, location)

        val productScansFromClient = productScanRepository.findByClientUuid(client.uuid)
        Assert.notEmpty(productScansFromClient, "Did not find product scan for client with uuid: " + client.uuid)
        Assert.isTrue(productScansFromClient.size == 1, "More than one product scan found for client with uuid: " + client.uuid)
        validateProductScan(productScansFromClient[0], productUuid, deviceId, location)
    }

    private fun validateProductScan(productScan: ProductScan, productUuid: String, deviceId: String, location: TestLocation?) {
        val client = clientDeviceRepository!!.findByIdHashed(CryptoUtil().hash(deviceId))!!.client

        Assert.isTrue(productScan.product.uuid == productUuid, "Product scan's product was not the product expected!")
        Assert.isTrue(productScan.client.uuid == client.uuid, "Product scan's client was not the client expected!")

        if (location != null && productScan.location != null) {
            Assert.isTrue(productScan.location!!.latitude == location.latitude, "Latitude for product scan not correct")
            Assert.isTrue(productScan.location!!.longitude == location.longitude, "Longitude for product scan not correct")
        } else {
            Assert.isNull(productScan.location, "Location for product scan was expected to be null, but was not!")
        }
    }
}
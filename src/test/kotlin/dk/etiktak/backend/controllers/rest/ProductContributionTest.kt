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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class ProductContributionTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "product/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
        client2DeviceId = createAndSaveClient()

        product1Uuid = createAndSaveProduct(client1DeviceId, "12345678a", Product.BarcodeType.EAN_13, "Test product 1")
        product2Uuid = createAndSaveProduct(client2DeviceId, "12345678b", Product.BarcodeType.UPC, "Test product 2")
    }

    /**
     * Test that we can edit a product name if we have sufficient trust level.
     */
    @Test
    fun editProductNameWithSufficientTrustLevel() {
        setClientTrustLevel(client1DeviceId, 0.7)
        setProductNameTrustScore(product1Uuid, 0.4)

        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Pepsi Cola")))
    }

    /**
     * Test that we cannot edit a product name if we don't have sufficient trust level.
     */
    @Test
    fun cannotEditProductNameWhenInsufficientTrustLevel() {
        setClientTrustLevel(client1DeviceId, 0.5)
        setProductNameTrustScore(product1Uuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
    }

    /**
     * Test that a client can trust vote a product name.
     */
    @Test
    fun trustVoteProductName() {
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that a client cannot trust vote more than once on same product name.
     */
    @Test
    fun cannotTrustVoteMoreThanOnceOnSameProductName() {
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
    }

    /**
     * Test that a client cannot vote on product names he edited himself.
     */
    @Test
    fun cannotTrustVoteOwnProductName() {
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that we can assign an existing company by UUID.
     */
    @Test
    fun assignExistingCompanyByUuid() {
        val companyUuid = createAndSaveCompany(client1DeviceId, "Existing company")

        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyUuid", companyUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Test product 1")))
                .andExpect(jsonPath("$.company.name", `is`("Existing company")))

        assertProductHasCompanyAssigned(product1Uuid, "Existing company")
    }

    /**
     * Test that we can assign an existing company by name (ignoring case).
     */
    @Test
    fun assignExistingCompanyByName() {
        createAndSaveCompany(client1DeviceId, "Existing company")

        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyName", "existing COMPANY"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Test product 1")))
                .andExpect(jsonPath("$.company.name", `is`("Existing company")))

        assertProductHasCompanyAssigned(product1Uuid, "Existing company")
    }

    /**
     * Test that we can create a new company while assigning company by name, if none with that name exists.
     */
    @Test
    fun assignNewCompanyByName() {
        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyName", "Some New Company"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.name", `is`("Test product 1")))
                .andExpect(jsonPath("$.company.name", `is`("Some New Company")))

        assertProductHasCompanyAssigned(product1Uuid, "Some New Company")
    }

    /**
     * Test that we cannot assign company to product without verified client.
     */
    @Test
    fun cannotAssignCompanyWithNonVerifiedClient() {
        val clientDeviceId = createAndSaveClient(verified = false)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", clientDeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyName", "Some New Company"))
    }

    /**
     * Test that we cannot assign same company twice.
     */
    @Test
    fun cannotAssignCompanyTwice() {

        // Assign once
        createAndSaveCompany(client1DeviceId, "Some Company", product1Uuid)

        // Assign twice
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyName", "Some Company"))
    }

    /**
     * Test that we can remove a company from a product.
     */
    @Test
    fun removeCompanyFromProduct() {
        val companyUuid = createAndSaveCompany(client1DeviceId, "Some Company", product1Uuid)
        assertProductHasCompanyAssigned(product1Uuid, "Some Company")

        mockMvc().perform(
                post(serviceEndpoint("remove/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyUuid", companyUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.companies", hasSize<Any>(0)))
    }

    /**
     * Test that we cannot remove a company from a product with insufficient trust level.
     */
    @Test
    fun cannotRemoveCompanyFromProductWithInsufficientTrustLevel() {
        setClientTrustLevel(client1DeviceId, 0.5)

        val companyUuid = createAndSaveCompany(client1DeviceId, "Some Company", product1Uuid)

        setProductCompanyContributionTrustScore(product1Uuid, companyUuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("remove/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyUuid", companyUuid))
    }

    /**
     * Test that we can add and remove and again add a company from a product.
     */
    @Test
    fun addAndRemoveAndAddCompanyFromProduct() {

        // Add company
        val companyUuid = createAndSaveCompany(client1DeviceId, "Some Company", product1Uuid)
        assertProductHasCompanyAssigned(product1Uuid, "Some Company")

        // Remove company
        mockMvc().perform(
                post(serviceEndpoint("remove/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyUuid", companyUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.companies", hasSize<Any>(0)))

        // Add company
        mockMvc().perform(
                post(serviceEndpoint("assign/company/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("companyUuid", companyUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.company.name", `is`("Some Company")))
    }



    fun assertProductHasNoCompanies(productUuid: String) {
        mockMvc().perform(
                MockMvcRequestBuilders.get(serviceEndpoint(""))
                        .param("uuid", productUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.companies", hasSize<Any>(0)))
    }

    fun assertProductHasCompanyAssigned(productUuid: String, companyName: String) {
        mockMvc().perform(
                MockMvcRequestBuilders.get(serviceEndpoint(""))
                        .param("uuid", productUuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.companies", hasSize<Any>(1)))
                .andExpect(jsonPath("$.product.companies[0].name", `is`(companyName)))
    }
}

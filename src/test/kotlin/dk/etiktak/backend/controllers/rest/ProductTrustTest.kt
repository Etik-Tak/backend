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
import dk.etiktak.backend.service.trust.ContributionService
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.util.Assert
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
     * Test that we can create a product with initial trust level.
     */
    @Test
    fun createProductWithInitialTrustLevel() {
        mockMvc().perform(
                post(serviceEndpoint("/create/"))
                        .header("clientuuid", client1Uuid)
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
        setProductNameTrustScore(product1Uuid, 0.4)

        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("clientuuid", client1Uuid)
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
        setProductNameTrustScore(product1Uuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("clientuuid", client1Uuid)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
    }

    /**
     * Test that a client can trust vote a product.
     */
    @Test
    fun trustVoteProduct() {
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("clientuuid", client2Uuid)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that a client cannot trust vote more than once on same product.
     */
    @Test
    fun cannotTrustVoteMoreThanOnceOnSameProduct() {
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("clientuuid", client2Uuid)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("clientuuid", client2Uuid)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
    }

    /**
     * Test that a client cannot vote on products he edited himself.
     */
    @Test
    fun cannotTrustVoteOwnProduct() {
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("clientuuid", client1Uuid)
                        .param("productUuid", product1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that client trust level and product trust score is updated when voted on product.
     */
    @Test
    fun clientTrustLevelAndProductTrustScoreIsUpdatedOnReceivingNewProductVote() {

        // Check initial trust values
        Assert.isTrue(
                clientTrustLevel(client1Uuid) == ContributionService.initialClientTrustLevel,
                "Client trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${clientTrustLevel(client1Uuid)}"
        )
        Assert.isTrue(
                productNameTrustLevel(product1Uuid) == ContributionService.initialClientTrustLevel,
                "Product trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${productNameTrustLevel(product1Uuid)}"
        )

        // Perform 5 trusted votes on product and see that trust increases
        trustVoteProduct(productUuid = product1Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta >= 0.0,
                    "Client trust level expected to increase, but delta was $clientTrustScoreDelta"
            )
            Assert.isTrue(
                    productTrustScoreDelta >= 0.0,
                    "Product trust level expected to increase, but delta was $productTrustScoreDelta"
            )
        })

        // Perform 5 not-trusted votes on product and see that trust decreases
        trustVoteProduct(productUuid = product1Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->

            Assert.isTrue(
                    clientTrustScoreDelta <= 0.0,
                    "Client trust level expected to decrease, but delta was $clientTrustScoreDelta"
            )
            Assert.isTrue(
                    productTrustScoreDelta <= 0.0,
                    "Product trust level expected to decrease, but delta was $productTrustScoreDelta"
            )
        })
    }

    /**
     * Test that client trust level is updated when other clients votes on product the client in question has voted on.
     */
    @Test
    fun clientTrustLevelIsUpdatedWhenReceivingNewProductVoteOnSameProductAsVotedOnByClient() {

        // Check initial trust values
        Assert.isTrue(
                clientTrustLevel(client1Uuid) == ContributionService.initialClientTrustLevel,
                "Client trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${clientTrustLevel(client1Uuid)}"
        )

        // Trust vote product
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("clientuuid", client1Uuid)
                        .param("productUuid", product2Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)

        // Perform 5 trusted votes on product and see that trust increases
        trustVoteProduct(productUuid = product2Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta >= 0.0,
                    "Client trust level expected to increase, but delta was $clientTrustScoreDelta"
            )
        })

        // Perform 5 not-trusted votes on product and see that trust decreases
        trustVoteProduct(productUuid = product2Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta <= 0.0,
                    "Client trust level expected to decrease, but delta was $clientTrustScoreDelta"
            )
        })
    }

    /**
     * Editing a product will reset trust score to client's trust level
     */
    @Test
    fun trustScoreResetsToClientsTrustLevelWhenEditing() {

        // Initial product trust score 0.5
        Assert.isTrue(
                productNameTrustLevel(product1Uuid) == ContributionService.initialClientTrustLevel,
                "Product trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${productNameTrustLevel(product1Uuid)}"
        )

        // Set client trust level to 0.7
        setClientTrustLevel(client2Uuid, 0.7)

        // Edit product
        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("clientuuid", client2Uuid)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.editableItems.name.trustScore", `is`(0.7)))
    }

    /**
     * Test that client trust level is NOT updated when a clients product edit is overwritten (re-edited) and the
     * product receives new votes.
     */
    @Test
    fun clientTrustLevelNotUpdatedWhenClientEditOverwrittenAndProductReceivesNewVotes() {

        // Edit product
        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("clientuuid", client1Uuid)
                        .param("productUuid", product2Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)

        // Perform 5 not-trusted votes on product and see that trust decreases
        trustVoteProduct(productUuid = product2Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta <= 0.0,
                    "Client trust level expected to decrease, but delta was $clientTrustScoreDelta"
            )
        })

        // Make another client edit product
        var initialClientTrust = clientTrustLevel(client1Uuid)

        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("clientuuid", client2Uuid)
                        .param("productUuid", product2Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)

        // Check trust unaffected
        Assert.isTrue(
                clientTrustLevel(client1Uuid) == initialClientTrust,
                "Client trust level expected to be the same as before edit: ${initialClientTrust}, but was ${clientTrustLevel(client1Uuid)}"
        )

        // Perform 5 trusted votes on product and see that trust stays the same
        trustVoteProduct(productUuid = product2Uuid, clientUuid = client1Uuid, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = {productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta == 0.0,
                    "Client trust level expected to stay the same, but delta was $clientTrustScoreDelta"
            )
        })
    }



    private fun trustVoteProduct(productUuid: String, clientUuid: String, createClient: Boolean = true, trustVoteType: TrustVote.TrustVoteType, count: Int,
                                 assertion: (productTrustScoreDelta: Double, clientTrustScoreDelta: Double) -> Unit) {

        var currentClientTrust = clientTrustLevel(clientUuid)
        var currentProductTrust = productNameTrustLevel(productUuid)

        for (i in 1..count) {

            // Create client
            val currentClientUuid = if (createClient) createAndSaveClient() else clientUuid

            // Vote
            mockMvc().perform(
                    post(serviceEndpoint("/trust/name/"))
                            .header("clientuuid", currentClientUuid)
                            .param("productUuid", productUuid)
                            .param("vote", trustVoteType.name))
                    .andExpect(status().isOk)

            // Assert
            assertion(productNameTrustLevel(productUuid) - currentProductTrust, clientTrustLevel(clientUuid) - currentClientTrust);

            // Update trust
            currentClientTrust = clientTrustLevel(clientUuid)
            currentProductTrust = productNameTrustLevel(productUuid)
        }
    }

    private fun setClientTrustLevel(clientUuid: String, trustLevel: Double) {
        val client = clientRepository!!.findByUuid(clientUuid)
        client!!.trustLevel = trustLevel
        clientRepository.save(client)
    }

    private fun clientTrustLevel(clientUuid: String): Double {
        return clientRepository!!.findByUuid(clientUuid)!!.trustLevel
    }

    private fun setProductNameTrustScore(productUuid: String, trustScore: Double) {
        val product = productRepository!!.findByUuid(productUuid)!!
        val contribution = productNameContributionRepository!!.findByProductUuidAndEnabled(product.uuid)[0]
        contribution.trustScore = trustScore
        productNameContributionRepository.save(contribution)
    }

    private fun productNameTrustLevel(productUuid: String): Double {
        val product = productRepository!!.findByUuid(productUuid)!!
        val contribution = productNameContributionRepository!!.findByProductUuidAndEnabled(product.uuid)[0]
        return contribution.trustScore
    }
}

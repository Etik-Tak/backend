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

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class TrustTest : BaseRestTest() {

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
     * Test that client trust level and product name trust score is updated when voted on product name that he contributed to.
     */
    @Test
    fun clientTrustLevelAndProductNameTrustScoreIsUpdatedOnReceivingNewProductNameVote() {

        // Check initial trust values
        Assert.isTrue(
                clientTrustLevel(client1DeviceId) == ContributionService.initialClientTrustLevel,
                "Client trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${clientTrustLevel(client1DeviceId)}"
        )
        Assert.isTrue(
                productNameTrustLevel(product1Uuid) == ContributionService.initialClientTrustLevel,
                "Product trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${productNameTrustLevel(product1Uuid)}"
        )

        // Perform 5 trusted votes on product name and see that trust increases
        trustVoteProductName(productUuid = product1Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta >= 0.0,
                    "Client trust level expected to increase, but delta was $clientTrustScoreDelta"
            )
            Assert.isTrue(
                    productTrustScoreDelta >= 0.0,
                    "Product trust level expected to increase, but delta was $productTrustScoreDelta"
            )
        })

        // Perform 5 not-trusted votes on product name and see that trust decreases
        trustVoteProductName(productUuid = product1Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->

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
     * Test that client trust level is updated when other clients votes on a product name the client in question has voted on.
     */
    @Test
    fun clientTrustLevelIsUpdatedWhenReceivingNewProductNameVoteOnSameProductNameAsVotedOnByClient() {

        // Check initial trust values
        Assert.isTrue(
                clientTrustLevel(client1DeviceId) == ContributionService.initialClientTrustLevel,
                "Client trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${clientTrustLevel(client1DeviceId)}"
        )

        // Trust vote product name
        mockMvc().perform(
                post(serviceEndpoint("trust/name/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product2Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)

        // Perform 5 trusted votes on product name and see that trust increases
        trustVoteProductName(productUuid = product2Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta >= 0.0,
                    "Client trust level expected to increase, but delta was $clientTrustScoreDelta"
            )
        })

        // Perform 5 not-trusted votes on product name and see that trust decreases
        trustVoteProductName(productUuid = product2Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta <= 0.0,
                    "Client trust level expected to decrease, but delta was $clientTrustScoreDelta"
            )
        })
    }

    /**
     * Editing a product name will reset trust score of name to client's trust level
     */
    @Test
    fun productNameTrustScoreResetsToClientsTrustLevelWhenEditing() {

        // Initial product name trust score 0.5
        Assert.isTrue(
                productNameTrustLevel(product1Uuid) == ContributionService.initialClientTrustLevel,
                "Product trust level expected to be ${ContributionService.initialClientTrustLevel}, but was ${productNameTrustLevel(product1Uuid)}"
        )

        // Set client trust level to 0.7
        setClientTrustLevel(client2DeviceId, 0.7)

        // Edit product name
        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productUuid", product1Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.product.editableItems[0].trustScore", `is`(0.7)))
    }

    /**
     * Test that client trust level is NOT updated when a clients product name edit is overwritten (re-edited) and the
     * product name receives new votes.
     */
    @Test
    fun clientTrustLevelNotUpdatedWhenClientEditOverwrittenAndProductNameReceivesNewVotes() {

        // Edit product name
        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("productUuid", product2Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)

        // Perform 5 not-trusted votes on product name and see that trust decreases
        trustVoteProductName(productUuid = product2Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.NotTrusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta <= 0.0,
                    "Client trust level expected to decrease, but delta was $clientTrustScoreDelta"
            )
        })

        // Make another client edit on product name
        val initialClientTrust = clientTrustLevel(client1DeviceId)

        mockMvc().perform(
                post(serviceEndpoint("edit/"))
                        .header("X-Auth-DeviceId", client2DeviceId)
                        .param("productUuid", product2Uuid)
                        .param("name", "Pepsi Cola"))
                .andExpect(status().isOk)

        // Check trust unaffected
        Assert.isTrue(
                clientTrustLevel(client1DeviceId) == initialClientTrust,
                "Client trust level expected to be the same as before edit: $initialClientTrust, but was ${clientTrustLevel(client1DeviceId)}"
        )

        // Perform 5 trusted votes on product name and see that trust stays the same
        trustVoteProductName(productUuid = product2Uuid, deviceId = client1DeviceId, trustVoteType = TrustVote.TrustVoteType.Trusted, count = 5, assertion = { productTrustScoreDelta, clientTrustScoreDelta ->
            Assert.isTrue(
                    clientTrustScoreDelta == 0.0,
                    "Client trust level expected to stay the same, but delta was $clientTrustScoreDelta"
            )
        })
    }



    private fun trustVoteProductName(productUuid: String, deviceId: String, createClient: Boolean = true, trustVoteType: TrustVote.TrustVoteType, count: Int,
                                     assertion: (productTrustScoreDelta: Double, clientTrustScoreDelta: Double) -> Unit) {

        var currentClientTrust = clientTrustLevel(deviceId)
        var currentProductTrust = productNameTrustLevel(productUuid)

        for (i in 1..count) {

            // Create client
            val currentDeviceId = if (createClient) createAndSaveClient() else deviceId

            // Vote
            mockMvc().perform(
                    post(serviceEndpoint("trust/name/"))
                            .header("X-Auth-DeviceId", currentDeviceId)
                            .param("productUuid", productUuid)
                            .param("vote", trustVoteType.name))
                    .andExpect(status().isOk)

            // Assert
            assertion(productNameTrustLevel(productUuid) - currentProductTrust, clientTrustLevel(deviceId) - currentClientTrust)

            // Update trust
            currentClientTrust = clientTrustLevel(deviceId)
            currentProductTrust = productNameTrustLevel(productUuid)
        }
    }
}

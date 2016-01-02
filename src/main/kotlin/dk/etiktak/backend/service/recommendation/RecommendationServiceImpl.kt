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

package dk.etiktak.backend.service.recommendation

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.recommendation.*
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.recommendation.ProductCategoryRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductLabelRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductRecommendationRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.infochannel.InfoChannelService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.util.*

@Service
class RecommendationServiceImpl @Autowired constructor(
        private val productRecommendationRepository: ProductRecommendationRepository,
        private val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository,
        private val productLabelRecommendationRepository: ProductLabelRecommendationRepository,
        private val infoChannelService: InfoChannelService,
        private val clientRepository: ClientRepository,
        private val infoChannelRepository: InfoChannelRepository) : RecommendationService {

    private val logger = LoggerFactory.getLogger(RecommendationServiceImpl::class.java)

    override fun getRecommendations(client: Client, product: Product): List<Recommendation> {
        return getRecommendations(infoChannelsFromClient(client), product)
    }

    fun getRecommendations(infoChannels: List<InfoChannel>, product: Product): List<Recommendation> {
        val productRecommendations = productRecommendationRepository.findByProductUuidAndInfoChannelUuidIn(
                product.uuid,
                infoChannelListToUuidList(infoChannels)
        )
        val productCategoryRecommendations = productCategoryRecommendationRepository.findByProductCategoryUuidInAndInfoChannelUuidIn(
                productCategoryListToUuidList(product.productCategories),
                infoChannelListToUuidList(infoChannels)
        )
        val productLabelRecommendations = productLabelRecommendationRepository.findByProductLabelUuidInAndInfoChannelUuidIn(
                productLabelListToUuidList(product.productLabels),
                infoChannelListToUuidList(infoChannels)
        )

        return productRecommendations + productCategoryRecommendations + productLabelRecommendations
    }


    /**
     * Create product recommendation.
     *
     * @param client         Client
     * @param infoChannel    Info channel
     * @param summary        Summary
     * @param score          Score
     * @param product        Product
     * @param modifyValues   Function called with modified client and info channel
     * @return               Created recommendation
     */
    override fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, product: Product,
                                      modifyValues: (Client, InfoChannel) -> Unit): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductRecommendation()
        recommendation.product = product

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedRecommandation = productRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel)

        return modifiedRecommandation
    }

    /**
     * Create product category recommendation.
     *
     * @param client           Client
     * @param infoChannel      Info channel
     * @param summary          Summary
     * @param score            Score
     * @param productCategory  Product category
     * @param modifyValues     Function called with modified client and info channel
     * @return                 Created recommendation
     */
    override fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productCategory: ProductCategory,
                                      modifyValues: (Client, InfoChannel) -> Unit): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductCategoryRecommendation()
        recommendation.productCategory = productCategory

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedRecommandation = productCategoryRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel)

        return modifiedRecommandation
    }

    /**
     * Create product label recommendation.
     *
     * @param client           Client
     * @param infoChannel      Info channel
     * @param summary          Summary
     * @param score            Score
     * @param productLabel     Product label
     * @param modifyValues     Function called with modified client and info channel
     * @return                 Created recommendation
     */
    override fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productLabel: ProductLabel,
                                      modifyValues: (Client, InfoChannel) -> Unit): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductLabelRecommendation()
        recommendation.productLabel = productLabel

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedRecommandation = productLabelRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel)

        return modifiedRecommandation
    }



    private fun setupRecommendation(client: Client, infoChannel: InfoChannel, recommendation: Recommendation,
                                    summary: String, score: RecommendationScore) {

        recommendation.summary = summary
        recommendation.score = score

        recommendation.creator = client
        client.recommendations.add(recommendation)

        recommendation.infoChannel = infoChannel
        infoChannel.recommendations.add(recommendation)
    }

    private fun infoChannelsFromClient(client: Client): List<InfoChannel> {
        val infoChannels: MutableList<InfoChannel> = ArrayList()
        for (infoChannelClient in client.infoChannelClients) {
            infoChannels.add(infoChannelClient.infoChannel)
        }
        return infoChannels
    }

    private fun infoChannelListToUuidList(infoChannels: List<InfoChannel>): List<String> {
        val infoChannelUuids: MutableList<String> = ArrayList()
        for (infoChannel in infoChannels) {
            infoChannelUuids.add(infoChannel.uuid)
        }
        return infoChannelUuids
    }

    private fun productCategoryListToUuidList(productCategories: Set<ProductCategory>): List<String> {
        val productCategoryUuids: MutableList<String> = ArrayList()
        for (productCategory in productCategories) {
            productCategoryUuids.add(productCategory.uuid)
        }
        return productCategoryUuids
    }

    private fun productLabelListToUuidList(productLabels: Set<ProductLabel>): List<String> {
        val productLabelUuids: MutableList<String> = ArrayList()
        for (productLabel in productLabels) {
            productLabelUuids.add(productLabel.uuid)
        }
        return productLabelUuids
    }
}

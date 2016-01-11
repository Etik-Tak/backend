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
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.product.ProductLabelRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.recommendation.ProductCategoryRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductLabelRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductRecommendationRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import java.util.*

@Service
@Transactional
open class RecommendationService @Autowired constructor(
        private val productRecommendationRepository: ProductRecommendationRepository,
        private val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository,
        private val productLabelRecommendationRepository: ProductLabelRecommendationRepository,
        private val productRepository: ProductRepository,
        private val productCategoryRepository: ProductCategoryRepository,
        private val productLabelRepository: ProductLabelRepository,
        private val infoChannelService: InfoChannelService,
        private val clientRepository: ClientRepository,
        private val infoChannelRepository: InfoChannelRepository) {

    private val logger = LoggerFactory.getLogger(RecommendationService::class.java)

    open fun getRecommendations(client: Client, product: Product): List<Recommendation> {
        return getRecommendations(followedInfoChannelsFromClient(client), product)
    }

    open fun getRecommendations(infoChannels: List<InfoChannel>, product: Product): List<Recommendation> {
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
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, product: Product,
                                  modifyValues: (Client, InfoChannel, Product) -> Unit = {client, infoChannel, product -> Unit}): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductRecommendation()
        recommendation.product = product

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        product.recommendations.add(recommendation)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProduct = productRepository.save(product)
        val modifiedRecommandation = productRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel, modifiedProduct)

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
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productCategory: ProductCategory,
                                  modifyValues: (Client, InfoChannel, ProductCategory) -> Unit = {client, infoChannel, productCategory -> Unit}): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductCategoryRecommendation()
        recommendation.productCategory = productCategory

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        productCategory.recommendations.add(recommendation)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProductCategory = productCategoryRepository.save(productCategory)
        val modifiedRecommandation = productCategoryRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel, modifiedProductCategory)

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
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productLabel: ProductLabel,
                                  modifyValues: (Client, InfoChannel, ProductLabel) -> Unit = {client, infoChannel, productLabel -> Unit}): Recommendation {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Create recommendation
        val recommendation = ProductLabelRecommendation()
        recommendation.productLabel = productLabel

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        productLabel.recommendations.add(recommendation)

        // Save it all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProductLabel = productLabelRepository.save(productLabel)
        val modifiedRecommandation = productLabelRecommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedInfoChannel, modifiedProductLabel)

        return modifiedRecommandation
    }



    open fun setupRecommendation(client: Client, infoChannel: InfoChannel, recommendation: Recommendation,
                                    summary: String, score: RecommendationScore) {

        recommendation.uuid = CryptoUtil().uuid()
        recommendation.summary = summary
        recommendation.score = score

        recommendation.creator = client
        client.recommendations.add(recommendation)

        recommendation.infoChannel = infoChannel
        infoChannel.recommendations.add(recommendation)
    }

    open fun followedInfoChannelsFromClient(client: Client): List<InfoChannel> {
        val infoChannels: MutableList<InfoChannel> = ArrayList()
        for (infoChannelFollower in client.followingInfoChannels) {
            infoChannels.add(infoChannelFollower.infoChannel)
        }
        return infoChannels
    }

    open fun infoChannelListToUuidList(infoChannels: List<InfoChannel>): List<String> {
        val infoChannelUuids: MutableList<String> = ArrayList()
        for (infoChannel in infoChannels) {
            infoChannelUuids.add(infoChannel.uuid)
        }
        return infoChannelUuids
    }

    open fun productCategoryListToUuidList(productCategories: Set<ProductCategory>): List<String> {
        val productCategoryUuids: MutableList<String> = ArrayList()
        for (productCategory in productCategories) {
            productCategoryUuids.add(productCategory.uuid)
        }
        return productCategoryUuids
    }

    open fun productLabelListToUuidList(productLabels: Set<ProductLabel>): List<String> {
        val productLabelUuids: MutableList<String> = ArrayList()
        for (productLabel in productLabels) {
            productLabelUuids.add(productLabel.uuid)
        }
        return productLabelUuids
    }
}

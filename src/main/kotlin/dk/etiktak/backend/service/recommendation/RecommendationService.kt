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

import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.product.ProductTag
import dk.etiktak.backend.model.recommendation.*
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.product.ProductLabelRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.product.ProductTagRepository
import dk.etiktak.backend.repository.recommendation.*
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.infosource.InfoSourceReferenceService
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.util.*

@Service
@Transactional
open class RecommendationService @Autowired constructor(
        private val infoSourceReferenceService: InfoSourceReferenceService,
        private val recommendationRepository: RecommendationRepository,
        private val productRecommendationRepository: ProductRecommendationRepository,
        private val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository,
        private val productLabelRecommendationRepository: ProductLabelRecommendationRepository,
        private val productTagRecommendationRepository: ProductTagRecommendationRepository,
        private val companyRecommendationRepository: CompanyRecommendationRepository,
        private val productRepository: ProductRepository,
        private val productCategoryRepository: ProductCategoryRepository,
        private val productLabelRepository: ProductLabelRepository,
        private val productTagRepository: ProductTagRepository,
        private val companyRepository: CompanyRepository,
        private val infoChannelService: InfoChannelService,
        private val infoChannelRepository: InfoChannelRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(RecommendationService::class.java)

    /**
     * Get recommendation from UUID.
     *
     * @param uuid  UUID
     * @return      Recommendation with given UUID
     */
    open fun getRecommendation(uuid: String): Recommendation? {
        return recommendationRepository.findByUuid(uuid)
    }

    /**
     * Get recommendations for a product for the given client.
     *
     * @param client   Client
     * @param product  Product
     * @return         List of recommendations for given product for all info channels followed by the given client
     */
    open fun getRecommendations(client: Client, product: Product): List<Recommendation> {
        return getRecommendations(followedInfoChannelsFromClient(client), product)
    }

    /**
     * Get recommendations for a product for the given info channels.
     *
     * @param infoChannels  Info channels to get recommendations for
     * @param product       Product
     * @return              List of recommendations for given product for all info channels
     */
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
        val productTagRecommendations = productTagRecommendationRepository.findByProductTagUuidInAndInfoChannelUuidIn(
                productTagListToUuidList(product.productTags),
                infoChannelListToUuidList(infoChannels)
        )
        val companyRecommendations = companyRecommendationRepository.findByCompanyProductsUuidAndInfoChannelUuidIn(
                product.uuid,
                infoChannelListToUuidList(infoChannels)
        )

        return productRecommendations +
                productCategoryRecommendations +
                productLabelRecommendations +
                productTagRecommendations +
                companyRecommendations
    }

    /**
     * Create product recommendation.
     *
     * @param client                   Client
     * @param infoChannel              Info channel
     * @param summary                  Summary
     * @param score                    Score
     * @param product                  Product
     * @param infoSourceReferenceUrls  Info source reference URLs
     * @param modifyValues             Function called with modified info channel and product
     * @return                         Created recommendation
     */
    @ClientVerified
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, product: Product,
                                  infoSourceReferenceUrls: List<String>,
                                  modifyValues: (InfoChannel, Product) -> Unit = {infoChannel, product -> Unit}): Recommendation {

        // Check recommendation does not already exists
        Assert.isNull(
                productRecommendationRepository.findByProductUuidAndInfoChannelUuid(product.uuid, infoChannel.uuid),
                "Info channel with UUID ${infoChannel.uuid} already has a recommandation for product with uuid: ${product.uuid}")

        // Create recommendation
        val recommendation = ProductRecommendation()
        recommendation.product = product

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        product.recommendations.add(recommendation)

        // Save it all
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProduct = productRepository.save(product)
        val modifiedRecommandation = productRecommendationRepository.save(recommendation)

        // Add info source references
        addInfoSourceReferencesToRecommendation(recommendation, infoSourceReferenceUrls)

        modifyValues(modifiedInfoChannel, modifiedProduct)

        return modifiedRecommandation
    }

    /**
     * Create product category recommendation.
     *
     * @param client                   Client
     * @param infoChannel              Info channel
     * @param summary                  Summary
     * @param score                    Score
     * @param productCategory          Product category
     * @param infoSourceReferenceUrls  Info source reference URLs
     * @param modifyValues             Function called with modified info channel and product category
     * @return                         Created recommendation
     */
    @ClientVerified
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productCategory: ProductCategory,
                                  infoSourceReferenceUrls: List<String>,
                                  modifyValues: (InfoChannel, ProductCategory) -> Unit = { infoChannel, productCategory -> Unit}): Recommendation {

        // Check recommendation does not already exists
        Assert.isNull(
                productCategoryRecommendationRepository.findByProductCategoryUuidAndInfoChannelUuid(productCategory.uuid, infoChannel.uuid),
                "Info channel with UUID ${infoChannel.uuid} already has a recommandation for category with uuid: ${productCategory.uuid}")

        // Create recommendation
        val recommendation = ProductCategoryRecommendation()
        recommendation.productCategory = productCategory

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        productCategory.recommendations.add(recommendation)

        // Save it all
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProductCategory = productCategoryRepository.save(productCategory)
        val modifiedRecommandation = productCategoryRecommendationRepository.save(recommendation)

        // Add info source references
        addInfoSourceReferencesToRecommendation(recommendation, infoSourceReferenceUrls)

        modifyValues(modifiedInfoChannel, modifiedProductCategory)

        return modifiedRecommandation
    }

    /**
     * Create product label recommendation.
     *
     * @param client                   Client
     * @param infoChannel              Info channel
     * @param summary                  Summary
     * @param score                    Score
     * @param productLabel             Product label
     * @param infoSourceReferenceUrls  Info source reference URLs
     * @param modifyValues             Function called with modified info channel and product label
     * @return                         Created recommendation
     */
    @ClientVerified
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productLabel: ProductLabel,
                                  infoSourceReferenceUrls: List<String>,
                                  modifyValues: (InfoChannel, ProductLabel) -> Unit = {infoChannel, productLabel -> Unit}): Recommendation {

        // Check recommendation does not already exists
        Assert.isNull(
                productLabelRecommendationRepository.findByProductLabelUuidAndInfoChannelUuid(productLabel.uuid, infoChannel.uuid),
                "Info channel with UUID ${infoChannel.uuid} already has a recommandation for label with uuid: ${productLabel.uuid}")

        // Create recommendation
        val recommendation = ProductLabelRecommendation()
        recommendation.productLabel = productLabel

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        productLabel.recommendations.add(recommendation)

        // Save it all
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProductLabel = productLabelRepository.save(productLabel)
        val modifiedRecommandation = productLabelRecommendationRepository.save(recommendation)

        // Add info source references
        addInfoSourceReferencesToRecommendation(recommendation, infoSourceReferenceUrls)

        modifyValues(modifiedInfoChannel, modifiedProductLabel)

        return modifiedRecommandation
    }

    /**
     * Create product tag recommendation.
     *
     * @param client                  Client
     * @param infoChannel             Info channel
     * @param summary                 Summary
     * @param score                   Score
     * @param productTag              Product tag
     * @param infoSourceReferenceUrls  Info source reference URLs
     * @param modifyValues            Function called with modified info channel and product tag
     * @return                        Created recommendation
     */
    @ClientVerified
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productTag: ProductTag,
                                  infoSourceReferenceUrls: List<String>,
                                  modifyValues: (InfoChannel, ProductTag) -> Unit = { infoChannel, productTag -> Unit}): Recommendation {

        // Check recommendation does not already exists
        Assert.isNull(
                productTagRecommendationRepository.findByProductTagUuidAndInfoChannelUuid(productTag.uuid, infoChannel.uuid),
                "Info channel with UUID ${infoChannel.uuid} already has a recommandation for tag with uuid: ${productTag.uuid}")

        // Create recommendation
        val recommendation = ProductTagRecommendation()
        recommendation.productTag = productTag

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        productTag.recommendations.add(recommendation)

        // Save it all
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedProductTag = productTagRepository.save(productTag)
        val modifiedRecommandation = productTagRecommendationRepository.save(recommendation)

        // Add info source references
        addInfoSourceReferencesToRecommendation(recommendation, infoSourceReferenceUrls)

        modifyValues(modifiedInfoChannel, modifiedProductTag)

        return modifiedRecommandation
    }

    /**
     * Create company recommendation.
     *
     * @param client                   Client
     * @param infoChannel              Info channel
     * @param summary                  Summary
     * @param score                    Score
     * @param company                  Company
     * @param infoSourceReferenceUrls  Info source reference URLs
     * @param modifyValues             Function called with modified info channel and company
     * @return                         Created recommendation
     */
    @ClientVerified
    open fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, company: Company,
                                  infoSourceReferenceUrls: List<String>,
                                  modifyValues: (InfoChannel, Company) -> Unit = { infoChannel, company -> Unit}): Recommendation {

        // Check recommendation does not already exists
        Assert.isNull(
                companyRecommendationRepository.findByCompanyUuidAndInfoChannelUuid(company.uuid, infoChannel.uuid),
                "Info channel with UUID ${infoChannel.uuid} already has a recommandation for company with uuid: ${company.uuid}")

        // Create recommendation
        var recommendation = CompanyRecommendation()
        recommendation.company = company

        setupRecommendation(client, infoChannel, recommendation, summary, score)

        company.recommendations.add(recommendation)

        // Save it all
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        val modifiedCompany = companyRepository.save(company)
        val modifiedRecommandation = companyRecommendationRepository.save(recommendation)

        // Add info source references
        addInfoSourceReferencesToRecommendation(recommendation, infoSourceReferenceUrls)

        modifyValues(modifiedInfoChannel, modifiedCompany)

        return modifiedRecommandation
    }


    /* Helper methods */

    open fun setupRecommendation(client: Client, infoChannel: InfoChannel, recommendation: Recommendation,
                                 summary: String, score: RecommendationScore) {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoChannel),
                "Client not member of info channel with UUID: ${infoChannel.uuid}")

        // Setup recommendation
        recommendation.uuid = CryptoUtil().uuid()
        recommendation.summary = summary
        recommendation.score = score

        recommendation.creator = client
        client.recommendations.add(recommendation)

        clientRepository.save(client)

        recommendation.infoChannel = infoChannel
        infoChannel.recommendations.add(recommendation)
    }

    open fun addInfoSourceReferencesToRecommendation(recommendation: Recommendation, infoSourceReferenceUrls: List<String>) {

        // Check that at least one info source reference is given
        Assert.isTrue(
                infoSourceReferenceUrls.size > 0,
                "At least one info source reference URL must be provided in order to create recommendation")

        for (infoSourceReferenceUrl in infoSourceReferenceUrls) {
            infoSourceReferenceService.createInfoSourceReference(recommendation.creator, recommendation, infoSourceReferenceUrl)
        }
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

    open fun productTagListToUuidList(productTags: Set<ProductTag>): List<String> {
        val productTagUuids: MutableList<String> = ArrayList()
        for (productTag in productTags) {
            productTagUuids.add(productTag.uuid)
        }
        return productTagUuids
    }
}

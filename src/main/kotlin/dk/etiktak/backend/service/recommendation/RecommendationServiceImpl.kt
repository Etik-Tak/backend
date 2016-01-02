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
import dk.etiktak.backend.model.recommendation.ProductCategoryRecommendation
import dk.etiktak.backend.model.recommendation.ProductLabelRecommendation
import dk.etiktak.backend.model.recommendation.ProductRecommendation
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.recommendation.ProductCategoryRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductLabelRecommendationRepository
import dk.etiktak.backend.repository.recommendation.ProductRecommendationRepository
import dk.etiktak.backend.repository.recommendation.RecommendationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class RecommendationServiceImpl @Autowired constructor(
        private val recommendationRepository: RecommendationRepository,
        private val productRecommendationRepository: ProductRecommendationRepository,
        private val productCategoryRecommendationRepository: ProductCategoryRecommendationRepository,
        private val productLabelRecommendationRepository: ProductLabelRecommendationRepository) : RecommendationService {

    private val logger = LoggerFactory.getLogger(RecommendationServiceImpl::class.java)

    override fun getRecommendations(client: Client): List<Recommendation> {
        return recommendationRepository.findByInfoChannelUuidIn(infoChannelUuidsFromClient(client))
    }

    override fun getProductRecommendations(client: Client): List<ProductRecommendation> {
        return productRecommendationRepository.findByInfoChannelUuidIn(infoChannelUuidsFromClient(client))
    }

    override fun getProductCategoryRecommendations(client: Client): List<ProductCategoryRecommendation> {
        return productCategoryRecommendationRepository.findByInfoChannelUuidIn(infoChannelUuidsFromClient(client))
    }

    override fun getProductLabelRecommendations(client: Client): List<ProductLabelRecommendation> {
        return productLabelRecommendationRepository.findByInfoChannelUuidIn(infoChannelUuidsFromClient(client))
    }



    override fun getRecommendations(infoChannel: InfoChannel): List<Recommendation> {
        return recommendationRepository.findByInfoChannelUuid(infoChannel.uuid)
    }

    override fun getProductRecommendations(infoChannel: InfoChannel): List<ProductRecommendation> {
        return productRecommendationRepository.findByInfoChannelUuid(infoChannel.uuid)
    }

    override fun getProductCategoryRecommendations(infoChannel: InfoChannel): List<ProductCategoryRecommendation> {
        return productCategoryRecommendationRepository.findByInfoChannelUuid(infoChannel.uuid)
    }

    override fun getProductLabelRecommendations(infoChannel: InfoChannel): List<ProductLabelRecommendation> {
        return productLabelRecommendationRepository.findByInfoChannelUuid(infoChannel.uuid)
    }



    override fun getRecommendations(infoChannels: List<InfoChannel>): List<Recommendation> {
        return recommendationRepository.findByInfoChannelUuidIn(infoChannelListToUuidList(infoChannels))
    }

    override fun getProductRecommendations(infoChannels: List<InfoChannel>): List<ProductRecommendation> {
        return productRecommendationRepository.findByInfoChannelUuidIn(infoChannelListToUuidList(infoChannels))
    }

    override fun getProductCategoryRecommendations(infoChannels: List<InfoChannel>): List<ProductCategoryRecommendation> {
        return productCategoryRecommendationRepository.findByInfoChannelUuidIn(infoChannelListToUuidList(infoChannels))
    }

    override fun getProductLabelRecommendations(infoChannels: List<InfoChannel>): List<ProductLabelRecommendation> {
        return productLabelRecommendationRepository.findByInfoChannelUuidIn(infoChannelListToUuidList(infoChannels))
    }



    override fun getProductRecommendationByUuid(uuid: String): ProductRecommendation? {
        return productRecommendationRepository.findByUuid(uuid)
    }

    override fun getProductRecommendationByProduct(product: Product): List<ProductRecommendation> {
        return productRecommendationRepository.findByProductUuid(product.uuid)
    }

    override fun getProductCategoryRecommendationByUuid(uuid: String): ProductCategoryRecommendation? {
        return productCategoryRecommendationRepository.findByUuid(uuid)
    }

    override fun getProductCategoryRecommendationByProduct(product: Product): List<ProductCategoryRecommendation> {
        return productCategoryRecommendationRepository.findByProductCategoryUuid(product.uuid)
    }

    override fun getProductLabelRecommendationByUuid(uuid: String): ProductLabelRecommendation? {
        return productLabelRecommendationRepository.findByUuid(uuid)
    }

    override fun getProductLabelRecommendationByProduct(product: Product): List<ProductLabelRecommendation> {
        return productLabelRecommendationRepository.findByProductLabelUuid(product.uuid)
    }



    private fun infoChannelUuidsFromClient(client: Client): List<String> {
        val infoChannelUuids: MutableList<String> = ArrayList()
        for (infoChannelClient in client.infoChannelClients) {
            infoChannelUuids.add(infoChannelClient.infoChannel.uuid)
        }
        return infoChannelUuids
    }

    private fun infoChannelListToUuidList(infoChannels: List<InfoChannel>): List<String> {
        val infoChannelUuids: MutableList<String> = ArrayList()
        for (infoChannel in infoChannels) {
            infoChannelUuids.add(infoChannel.uuid)
        }
        return infoChannelUuids
    }
}

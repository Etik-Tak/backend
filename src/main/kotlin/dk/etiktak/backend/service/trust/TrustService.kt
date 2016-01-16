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

package dk.etiktak.backend.service.trust

import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.trust.TrustVoteType
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.trust.ProductTrustVoteRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
@Transactional
open class TrustService @Autowired constructor(
        private val productTrustVoteRepository: ProductTrustVoteRepository) {

    private val logger = LoggerFactory.getLogger(TrustService::class.java)

    open fun assertSufficientTrustToEditProduct(client: Client, product: Product) {
        Assert.isTrue(
                client.trustLevel >= product.correctnessTrust,
                "Client with UUID ${client.uuid} does not have sufficient trust level to edit product with name ${product.name} and UUID ${product.uuid}. " +
                        "Client trust: ${client.trustLevel}. " +
                        "Product correctness trust: ${product.correctnessTrust}."
        )
    }

    /**
     * Calculate product correctness trust score based on votes.
     *
     * @param product   Product
     * @return          Correctness trust score
     */
    open fun correctnessTrustForProduct(product: Product): Double {
        val trustedVotesCount = productTrustVoteRepository.countByVoteAndProductUuid(TrustVoteType.Trusted, product.uuid)
        val untrustedVotesCount = productTrustVoteRepository.countByVoteAndProductUuid(TrustVoteType.NotTrusted, product.uuid)
        val totalVotesCount = trustedVotesCount + untrustedVotesCount

        if (totalVotesCount > 0) {
            val votedCorrectnessTrust = trustedVotesCount.toDouble() / totalVotesCount
            val votedCorrectnessTrustWeight = 0.5 // TODO!

            val creatorCorrectnessTrust = product.initialCorrectnessTrust
            val creatorCorrectnessTrustWeight = 1.0 - votedCorrectnessTrustWeight

            return (votedCorrectnessTrust * votedCorrectnessTrustWeight) + (creatorCorrectnessTrust * creatorCorrectnessTrustWeight)
        } else {
            return product.initialCorrectnessTrust
        }
    }

    /**
     * Recalculates client trust level.
     *
     * @param client        Client
     * @param modifyValues  Function called with modified client
     */
    open fun recalculateClientTrustLevel(client: Client, modifyValues: (Client) -> Unit = {}) {
        // TODO!
    }
}
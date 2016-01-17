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

import dk.etiktak.backend.model.trust.TrustItem
import dk.etiktak.backend.model.trust.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.trust.TrustItemRepository
import dk.etiktak.backend.repository.trust.TrustVoteRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
@Transactional
open class TrustService @Autowired constructor(
        private val trustItemRepository: TrustItemRepository,
        private val trustVoteRepository: TrustVoteRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(TrustService::class.java)

    /**
     * Checks that client has succicient trust to edit given trust item.
     *
     * @param client     Client
     * @param trustItem  Trust item
     */
    open fun assertSufficientTrustToEditTrustItem(client: Client, trustItem: TrustItem) {
        Assert.isTrue(
                client.trustLevel >= trustItem.trustScore,
                "Client with UUID ${client.uuid} does not have sufficient trust level to edit trust item with UUID ${trustItem.uuid}. " +
                        "Client trust: ${client.trustLevel}. " +
                        "Trust item score: ${trustItem.trustScore}."
        )
    }

    /**
     * Create new trust item.
     *
     * @param client          Client
     * @param trustItemType   Trust item type, fx. Product
     * @param trustUuid       Target trust item uuid, fx. product uuid
     * @param modifyValues    Function called with modified client
     * @return                Created trust item
     */
    @ClientVerified
    open fun createTrustItem(client: Client, trustItemType: TrustItem.TrustItemType, trustUuid: String, modifyValues: (Client) -> Unit = {}): TrustItem {

        // Create trust item
        val trustItem = TrustItem()
        trustItem.uuid = CryptoUtil().uuid()
        trustItem.creator = client
        trustItem.type = TrustItem.TrustItemType.Product
        trustItem.trustUuid = trustUuid
        trustItem.initialTrustScore = client.trustLevel

        // Glue it together
        client.trustItems.add(trustItem)

        // Save it all
        var modifiedClient = clientRepository.save(client)
        var modifiedTrustItem = trustItemRepository.save(trustItem)

        modifyValues(modifiedClient)

        return modifiedTrustItem
    }

    /**
     * Create trust vote on trust item.
     *
     * @param client        Client
     * @param trustItem     Trust item
     * @param vote          Vote
     * @param modifyValues  Function called with modified client and product
     * @return              Trust vote
     */
    @ClientVerified
    open fun trustVoteItem(client: Client, trustItem: TrustItem, vote: TrustVote.TrustVoteType, modifyValues: (Client, TrustItem) -> Unit = { client, trustItem -> Unit}): TrustVote {

        // Clients cannot vote on their "own" item
        Assert.isTrue(
                !trustItem.creator.uuid.equals(client.uuid),
                "Client with uuid ${client.uuid} cannot vote on his own item with uuid ${trustItem.uuid}"
        )

        // Clients can only vote once on same item
        Assert.isNull(
                trustVoteRepository.findByClientUuidAndTrustItemUuid(client.uuid, trustItem.uuid),
                "Client with uuid ${client.uuid} already trust voted trust item with uuid ${trustItem.uuid}"
        )

        // Create trust vote
        val trustVote = TrustVote()
        trustVote.vote = vote
        trustVote.client = client

        // Glue it together
        trustVote.trustItem = trustItem
        trustItem.trustVotes.add(trustVote)
        client.trustVotes.add(trustVote)

        // Save it all
        var modifiedVote = trustVoteRepository.save(trustVote)
        var modifiedClient = clientRepository.save(client)

        // Recalculate trust
        var modifiedTrustItem = trustItem

        updateTrust(trustItem, modifyValues = {trustItem -> modifiedTrustItem = trustItem})

        modifyValues(clientRepository.findByUuid(modifiedClient.uuid)!!, modifiedTrustItem)

        return modifiedVote
    }

    /**
     * Recalculate trust score based on votes and initial trust score.
     *
     * @param trustItem     Trust item
     * @param modifyValues  Function called with modified trust item
     */
    open fun updateTrust(trustItem: TrustItem, modifyValues: (TrustItem) -> Unit = {}) {

        // Update trust item
        val trustedVotesCount = trustItemRepository.countByUuidAndTrustVotesVote(trustItem.uuid, TrustVote.TrustVoteType.Trusted)
        val untrustedVotesCount = trustItemRepository.countByUuidAndTrustVotesVote(trustItem.uuid, TrustVote.TrustVoteType.NotTrusted)
        val totalVotesCount = trustedVotesCount + untrustedVotesCount

        if (totalVotesCount > 0) {
            val votedTrustScore = trustedVotesCount.toDouble() / totalVotesCount
            val votedTrustScoreWeight = 0.5 // TODO!

            val initialTrustScore = trustItem.initialTrustScore
            val initialTrustScoreWeight = 1.0 - votedTrustScoreWeight

            trustItem.trustScore = (votedTrustScore * votedTrustScoreWeight) + (initialTrustScore * initialTrustScoreWeight)
        } else {
            trustItem.trustScore = trustItem.initialTrustScore
        }

        val modifiedTrustItem = trustItemRepository.save(trustItem)

        // Recalculate all vote contributers trust level
        for (trustVote in trustItem.trustVotes) {
            recalculateClientTrustLevel(trustVote.client)
        }

        modifyValues(modifiedTrustItem)
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
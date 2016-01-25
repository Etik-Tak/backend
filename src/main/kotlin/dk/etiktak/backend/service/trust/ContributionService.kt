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

import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.contribution.ContributionRepository
import dk.etiktak.backend.repository.contribution.TrustVoteRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
@Transactional
open class ContributionService @Autowired constructor(
        private val contributionRepository: ContributionRepository,
        private val trustVoteRepository: TrustVoteRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(ContributionService::class.java)

    companion object {
        val initialClientTrustLevel = 0.5
        val TrustScoreContributionDelta = 0.05
    }

    /**
     * Returns whether the given client has succicient trust to edit given contribution.
     *
     * @param client        Client
     * @param contribution  Contribution
     * @return              True, if client can edit contribution, or else false. If contribution is null, true is returned
     */
    open fun hasSufficientTrustToEditContribution(client: Client, contribution: Contribution?): Boolean {
        if (contribution != null) {
            return client.trustLevel >= contribution.trustScore - TrustScoreContributionDelta
        } else {
            return true
        }
    }

    /**
     * Checks that client has succicient trust to edit given contribution.
     *
     * @param client        Client
     * @param contribution  Contribution
     */
    open fun assertSufficientTrustToEditContribution(client: Client, contribution: Contribution) {
        Assert.isTrue(
                hasSufficientTrustToEditContribution(client, contribution),
                "Client with UUID ${client.uuid} does not have sufficient trust level to edit contribution with UUID ${contribution.uuid}. " +
                        "Client trust: ${client.trustLevel}. " +
                        "Contribution score: ${contribution.trustScore}."
        )
    }

    /**
     * Create trust vote on contribution.
     *
     * @param inClient        Client
     * @param inContribution  Contribution
     * @param vote            Vote
     * @param modifyValues    Function called with modified client and contribution
     * @return                Trust vote
     */
    @ClientVerified
    open fun trustVoteItem(inClient: Client, inContribution: Contribution, vote: TrustVote.TrustVoteType, modifyValues: (Client, Contribution) -> Unit = { client, contribution -> Unit}): TrustVote {

        var client = inClient
        var contribution = inContribution

        // Clients cannot vote on their own contribution
        Assert.isTrue(
                !contribution.client.uuid.equals(client.uuid),
                "Client with uuid ${client.uuid} cannot vote on his own contribution with uuid ${contribution.uuid}"
        )

        // Clients can only vote once on same item
        Assert.isNull(
                trustVoteRepository.findByClientUuidAndContributionUuid(client.uuid, contribution.uuid),
                "Client with uuid ${client.uuid} already trust voted contribution with uuid ${contribution.uuid}"
        )

        // Create trust vote
        var trustVote = TrustVote()
        trustVote.vote = vote
        trustVote.client = client

        // Glue it together
        trustVote.contribution = contribution
        contribution.trustVotes.add(trustVote)
        client.trustVotes.add(trustVote)

        // Save it all
        trustVote = trustVoteRepository.save(trustVote)
        client = clientRepository.save(client)
        contribution = contributionRepository.save(contribution)

        // Recalculate trust
        updateTrust(contribution, modifyValues = {modifiedContribution -> contribution = modifiedContribution})

        modifyValues(clientRepository.findByUuid(client.uuid)!!, contribution)

        return trustVote
    }

    /**
     * Recalculate trust score based on votes and client trust score.
     *
     * @param inContribution   Contribution
     * @param modifyValues     Function called with modified contribution
     */
    open fun updateTrust(inContribution: Contribution, modifyValues: (Contribution) -> Unit = {}) {

        var contribution = inContribution

        // Count votes
        val trustedVotesCount = contributionRepository.countByUuidAndTrustVotesVote(contribution.uuid, TrustVote.TrustVoteType.Trusted)
        val untrustedVotesCount = contributionRepository.countByUuidAndTrustVotesVote(contribution.uuid, TrustVote.TrustVoteType.NotTrusted)
        val totalVotesCount = trustedVotesCount + untrustedVotesCount

        // Update trust item
        if (totalVotesCount > 0) {

            // Voted trust score is simply the percentage of trusted votes out of total number of votes.
            // Weight has linear growth up until 20 votes, reaching its maximum at 95%.
            val votedTrustScore = trustedVotesCount.toDouble() / totalVotesCount.toDouble()
            val votedTrustScoreWeight = linearGrowthTrustWeight(totalVotesCount)

            val clientTrustLevel = contribution.client.trustLevel
            val clientTrustLevelWeight = 1.0 - votedTrustScoreWeight

            // Calculate trust by linearly interpolating between initial trust and voted trust
            contribution.trustScore = (votedTrustScore * votedTrustScoreWeight) + (clientTrustLevel * clientTrustLevelWeight)

        } else {

            // Use client trust if no votes
            contribution.trustScore = contribution.client.trustLevel
        }

        // Save contribution
        contribution = contributionRepository.save(contribution)

        // Recalculate creators trust level
        recalculateClientTrustLevel(contribution.client)

        // Recalculate all vote contributers trust level
        for (trustVote in contribution.trustVotes) {
            recalculateClientTrustLevel(trustVote.client)
        }

        modifyValues(contribution)
    }

    /**
     * Recalculates client trust level.
     *
     * @param inClient      Client
     * @param modifyValues  Function called with modified client
     */
    open fun recalculateClientTrustLevel(inClient: Client, modifyValues: (Client) -> Unit = {}) {

        var client = inClient

        // Reset total score and weight
        var totalScore = 0.0
        var totalWeight = 0.0

        // Initial trust level
        totalScore += initialClientTrustLevel
        totalWeight += 1.0

        // Find client contributions
        val contributions = contributionRepository.findByClientUuid(client.uuid)

        // Find contributions voted on by client
        val votedContributions = contributionRepository.findByTrustVotesClientUuid(client.uuid)

        // Add trust from client contributions
        for (contribution in contributions) {
            totalScore += contribution.trustScore
            totalWeight += 1.0
        }

        // Add trust from contributions(voted on by client
        for (contribution in votedContributions) {

            // Count trusted and not-trusted votes
            val trustedVoteCount = trustVoteRepository.countByVoteAndContributionUuid(TrustVote.TrustVoteType.Trusted, contribution.uuid)
            val notTrustedVoteCount = trustVoteRepository.countByVoteAndContributionUuid(TrustVote.TrustVoteType.NotTrusted, contribution.uuid)

            // If none, ignore trust item
            if (trustedVoteCount == 0L && notTrustedVoteCount == 0L) {
                continue
            }

            // Find client's trust vote
            val actualVote = trustVoteRepository.findByClientUuidAndContributionUuid(client.uuid, contribution.uuid)!!

            // Calculate weight
            val ratio = Math.min(trustedVoteCount, notTrustedVoteCount).toDouble() / Math.max(trustedVoteCount, notTrustedVoteCount).toDouble()
            val weight = Math.pow(1.0 - ratio, 3.0) * linearGrowthTrustWeight(trustedVoteCount + notTrustedVoteCount)

            // Calculate score
            val majorityVoteType = if (trustedVoteCount > notTrustedVoteCount) TrustVote.TrustVoteType.Trusted else TrustVote.TrustVoteType.NotTrusted
            val score = if (actualVote.vote == majorityVoteType) 1.0 else 0.0

            // Add to total
            totalScore += score * weight
            totalWeight += weight
        }

        // Average on trust contributions
        val trustLevel = totalScore / totalWeight

        // Update client
        client.trustLevel = trustLevel

        // Save client
        client = clientRepository.save(client)

        modifyValues(client)
    }

    /**
     * Calculates the weight of the voted trust.
     *
     * @param votesCount  Number of trust votes
     * @return            Weight of voted trust from 0 to 1
     */
    open fun linearGrowthTrustWeight(votesCount: Long): Double {
        val votedTrustWeightMax = 0.95
        val votedTrustWeightGrowth = votedTrustWeightMax / 20.0

        return Math.min(votesCount * votedTrustWeightGrowth, votedTrustWeightMax)
    }

    /**
     * Returns the unique contribution from the given list. Asserts that the list has size more no than one.
     *
     * @param contributions    List of contributions
     * @return                 Element at position 0, if any
     */
    open fun <T: Contribution> uniqueContribution(contributions: List<T>): T? {
        Assert.isTrue(
                contributions.size <= 1,
                "Expected only one active contribution, but found ${contributions.size}"
        )
        return if (contributions.size > 0) contributions[0] else null
    }
}
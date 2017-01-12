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

package dk.etiktak.backend.service.trust

import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.contribution.ReferenceContribution
import dk.etiktak.backend.model.contribution.TextContribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.contribution.ContributionRepository
import dk.etiktak.backend.repository.contribution.ReferenceContributionRepository
import dk.etiktak.backend.repository.contribution.TextContributionRepository
import dk.etiktak.backend.repository.contribution.TrustVoteRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
@Transactional
open class ContributionService @Autowired constructor(
        private val contributionRepository: ContributionRepository,
        private val textContributionRepository: TextContributionRepository,
        private val referenceContributionRepository: ReferenceContributionRepository,
        private val trustVoteRepository: TrustVoteRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(ContributionService::class.java)

    companion object {
        val initialClientTrustLevel = 0.5
        val trustScoreContributionDelta = 0.05
        val votedTrustWeightMax = 0.95
        val votedTrustWeightLinearGrowthUpUntil = 5.0
    }

    /**
     * Returns the current text contribution.
     *
     * @param contributionType  Contribution type, fx. CompanyName
     * @param subjectUuid       Subject UUID, fx. company UUID
     * @return                  Current text contribution
     */
    open fun currentTextContribution(contributionType: Contribution.ContributionType, subjectUuid: String): TextContribution? {
        val currentContributions = textContributionRepository.findBySubjectUuidAndType(subjectUuid, contributionType, PageRequest(0, 1, Sort(Sort.Direction.DESC, "creationTime")))
        return if (currentContributions.size == 1) currentContributions[0] else null
    }

    /**
     * Asserts that a contribution with given subject UUID and reference UUID is not already present.
     *
     * @param subjectUuid     Subject UUID, fx. product UUID
     * @param referenceUuid   Reference UUID, fx. product tag UUID
     */
    fun assertReferenceContributionNotPresent(subjectUuid: String, referenceUuid: String) {
        val contributions = referenceContributionRepository.findBySubjectUuidAndReferenceUuidAndEnabled(subjectUuid, referenceUuid)
        Assert.isTrue(
                contributions.isEmpty(),
                "Contribution with subject UUID $subjectUuid and reference UUID $referenceUuid already present")
    }

    /**
     * Creates a new text contribution.
     *
     * @param contributionType  Contribution type, fx. CompanyName
     * @param inClient          Client
     * @param subjectUuid       Subject UUID, fx. company UUID
     * @param text              Text
     * @param modifyValues      Function called with modified client
     * @return                  Created contribution
     */
    open fun createTextContribution(contributionType: Contribution.ContributionType, inClient: Client, subjectUuid: String, text: String, modifyValues: (Client) -> Unit = {}): Contribution {

        var client = inClient

        // Check sufficient trust
        val currentContribution = currentTextContribution(contributionType, subjectUuid)
        currentContribution?.let {
            assertSufficientTrustToEditContribution(client, currentContribution)
        }

        // Create contribution
        var contribution = TextContribution()
        contribution.uuid = CryptoUtil().uuid()
        contribution.type = contributionType
        contribution.subjectUuid = subjectUuid
        contribution.text = text

        // Glue it together
        contribution.client = client
        client.contributions.add(contribution)

        // Save it all
        client = clientRepository.save(client)
        contribution = contributionRepository.save(contribution)

        // Update trust
        updateTrust(contribution)

        modifyValues(client)

        return contribution
    }

    /**
     * Creates a new reference contribution.
     *
     * @param contributionType  Contribution type, fx. CompanyName
     * @param inClient          Client
     * @param subjectUuid       Subject UUID, fx. product UUID
     * @param referenceUuid     Reference UUID, fx. product tag UUID
     * @param modifyValues      Function called with modified client
     * @return                  Created contribution
     */
    open fun createReferenceContribution(contributionType: Contribution.ContributionType, inClient: Client, subjectUuid: String, referenceUuid: String, modifyValues: (Client) -> Unit = {}): Contribution {

        var client = inClient

        // Make sure it's not already present and enabled
        assertReferenceContributionNotPresent(subjectUuid, referenceUuid)

        // Create contribution
        var contribution = ReferenceContribution()
        contribution.uuid = CryptoUtil().uuid()
        contribution.type = contributionType
        contribution.subjectUuid = subjectUuid
        contribution.referenceUuid = referenceUuid

        // Glue it together
        contribution.client = client
        client.contributions.add(contribution)

        // Save it all
        client = clientRepository.save(client)
        contribution = contributionRepository.save(contribution)

        // Update trust
        updateTrust(contribution)

        modifyValues(client)

        return contribution
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
            return client.trustLevel >= contribution.trustScore - trustScoreContributionDelta
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
                contribution.client.uuid != client.uuid,
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

            // Voted trust score is simply the percentage of trusted votes out of total number of votes. Weight has linear growth.
            val votedTrustScore = trustedVotesCount.toDouble() / totalVotesCount.toDouble()
            val votedTrustScoreWeight = linearGrowthTrustWeight(totalVotesCount)

            // Client trust level
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

        // Add trust from contributions voted on by client
        for (contribution in votedContributions) {

            // Count trusted and not-trusted votes
            val trustedVoteCount = trustVoteRepository.countByVoteAndContributionUuid(TrustVote.TrustVoteType.Trusted, contribution.uuid)
            val notTrustedVoteCount = trustVoteRepository.countByVoteAndContributionUuid(TrustVote.TrustVoteType.NotTrusted, contribution.uuid)
            val totalVoteCount = trustedVoteCount + notTrustedVoteCount

            // If none, ignore trust item
            if (trustedVoteCount == 0L && notTrustedVoteCount == 0L) {
                continue
            }

            // Find client's trust vote
            val actualVote = trustVoteRepository.findByClientUuidAndContributionUuid(client.uuid, contribution.uuid)!!

            // Weight is lowered if vote is of type trusted. This is done in an attempt to mitigate clients from easily
            // gaining higher trust level from just contributing with votes that provide no real value.
            val weightImpact = if (actualVote.vote == TrustVote.TrustVoteType.Trusted) 0.75 else 1.0

            // Calculate weight. Weight increases quadratic with number of votes.
            val ratio = Math.min(trustedVoteCount, notTrustedVoteCount).toDouble() / Math.max(trustedVoteCount, notTrustedVoteCount).toDouble()
            val weight = Math.pow(1.0 - ratio, 3.0) * linearGrowthTrustWeight(totalVoteCount) * weightImpact

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
        val votedTrustWeightGrowth = votedTrustWeightMax / votedTrustWeightLinearGrowthUpUntil

        return Math.min(votesCount * votedTrustWeightGrowth, votedTrustWeightMax)
    }

    /**
     * Returns the unique contribution from the given list. Asserts that the list has size no more than one.
     *
     * @param contributions    List of contributions
     * @return                 Element at position 0, if any
     */
    open fun <T: Contribution> uniqueContribution(contributions: List<T>): T? {
        Assert.isTrue(
                contributions.size <= 1,
                "Expected only one active contribution, but found ${contributions.size}"
        )
        return if (contributions.isNotEmpty()) contributions[0] else null
    }
}
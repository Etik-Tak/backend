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

package dk.etiktak.backend.service.infosource

import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.contribution.TextContribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceDomain
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceDomainRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.net.URI

@Service
@Transactional
open class InfoSourceService @Autowired constructor(
        private val infoSourceRepository: InfoSourceRepository,
        private val infoSourceDomainRepository: InfoSourceDomainRepository,
        private val contributionService: ContributionService) {

    private val logger = LoggerFactory.getLogger(InfoSourceService::class.java)

    /**
     * Finds an info source from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info source with given UUID
     */
    open fun getInfoSourceByUuid(uuid: String): InfoSource? {
        return infoSourceRepository.findByUuid(uuid)
    }

    /**
     * Finds an info source from the given url.
     *
     * @param url   URL
     * @return      Info source with given url
     */
    open fun getInfoSourceByUrl(url: String): InfoSource? {
        val domain = domainFromUrl(url)
        return infoSourceRepository.findByDomainsDomain(domain)
    }

    /**
     * Creates an info source.
     *
     * @param inClient       Creator
     * @param domains        Domains
     * @param name           Name of info source
     * @param modifyValues   Function called with modified client
     * @return               Created info source
     */
    @ClientVerified
    open fun createInfoSource(inClient: Client, domains: List<String>, name: String? = null, modifyValues: (Client) -> Unit = {}): InfoSource {
        var client = inClient

        // Validate domains
        for (domain in domains) {
            validateDomain(domain)
        }

        // Create info source
        var infoSource = InfoSource()
        infoSource.uuid = CryptoUtil().uuid()
        infoSource.name = name

        // Create domains
        for (domain in domains) {
            val infoSourceDomain = InfoSourceDomain()
            infoSourceDomain.uuid = CryptoUtil().uuid()
            infoSourceDomain.domain = domain
            infoSourceDomain.infoSource = infoSource

            infoSource.domains.add(infoSourceDomain)
        }

        // Save it all
        infoSourceRepository.save(infoSource)
        for (infoSourceDomain in infoSource.domains) {
            infoSourceDomainRepository.save(infoSourceDomain)
        }

        // Create name contribution
        editInfoSourceName(client, infoSource, name ?: "", modifyValues = {modifiedClient, modifiedInfoSource -> client = modifiedClient; infoSource = modifiedInfoSource})

        modifyValues(client)

        return infoSource
    }

    /**
     * Creates an info source from a given URL.
     *
     * @param client         Creator
     * @param url            URL
     * @return               Created info source
     */
    @ClientVerified
    open fun createInfoSourceFromUrl(client: Client, url: String): InfoSource {
        return createInfoSource(client, arrayListOf(domainFromUrl(url)))
    }

    /**
     * Creates an domain from the given url.
     *
     * @param url    URL
     * @return       Domain
     * @throws       Exception if url is not valid
     */
    open fun domainFromUrl(url: String): String {
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must not be empty")

        val uri = URI(url)
        return uri.host
    }

    /**
     * Validates an info source domain.
     *
     * @param domain      Domain
     * @throws            Exception if domain does not validate
     */
    open fun validateDomain(domain: String) {
        Assert.isTrue(
                !StringUtils.isEmpty(domain),
                "Domain must not be empty")

        // Validate domain by creating an URI
        URI(domain)
    }

    /**
     * Edits an info source name.
     *
     * @param inClient        Client
     * @param inInfoSource    Info source
     * @param name            Name of info source
     * @param modifyValues    Function called with modified client and info source
     * @return                Info source name contribution
     */
    @ClientVerified
    open fun editInfoSourceName(inClient: Client, inInfoSource: InfoSource, name: String, modifyValues: (Client, InfoSource) -> Unit = {client, infoSource -> Unit}): Contribution {

        var client = inClient
        var infoSource = inInfoSource

        // Create contribution
        val contribution = contributionService.createTextContribution(Contribution.ContributionType.InfoSourceName, client, infoSource.uuid, name, modifyValues = {modifiedClient -> client = modifiedClient})

        // Edit name
        infoSource.name = name
        infoSource = infoSourceRepository.save(infoSource)

        modifyValues(client, infoSource)

        return contribution
    }

    /**
     * Returns whether the given client can edit the name of the info source.
     *
     * @param client      Client
     * @param infoSource  Info source
     * @return            Yes, if the given client can edit the name of the info source, or else false
     */
    open fun canEditInfoSourceName(client: Client, infoSource: InfoSource): Boolean {
        return contributionService.hasSufficientTrustToEditContribution(client, infoSourceNameContribution(infoSource))
    }

    /**
     * Returns the info source name contribution which is currently active.
     *
     * @param infoSource  Info source
     * @return            info source name contribution
     */
    open fun infoSourceNameContribution(infoSource: InfoSource): TextContribution? {
        return contributionService.currentTextContribution(Contribution.ContributionType.InfoSourceName, infoSource.uuid)
    }

    /**
     * Trust votes info source name.
     *
     * @param client            Client
     * @param infoSource        Info source
     * @param vote              Vote
     * @param modifyValues      Function called with modified client
     * @return                  Trust vote
     */
    open fun trustVoteInfoSourceName(client: Client, infoSource: InfoSource, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = infoSourceNameContribution(infoSource)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }
}
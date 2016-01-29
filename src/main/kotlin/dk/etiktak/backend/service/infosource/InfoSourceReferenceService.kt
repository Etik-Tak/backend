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

import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.recommendation.RecommendationRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
@Transactional
open class InfoSourceReferenceService @Autowired constructor(
        private val infoChannelService: InfoChannelService,
        private val infoSourceService: InfoSourceService,
        private val infoSourceReferenceRepository: InfoSourceReferenceRepository,
        private val infoSourceRepository: InfoSourceRepository,
        private val recommendationRepository: RecommendationRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(InfoSourceReferenceService::class.java)

    /**
     * Finds an info source reference from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info source reference with given UUID
     */
    open fun getInfoSourceReferenceByUuid(uuid: String): InfoSourceReference? {
        return infoSourceReferenceRepository.findByUuid(uuid)
    }

    /**
     * Creates an info source reference.
     *
     * @param client           Creator
     * @param recommendation   Recommendation
     * @param url              Reference URL
     * @param title            Title
     * @param modifyValues     Function called with modified client and recommendation
     * @return                 Created info source reference
     */
    @ClientVerified
    open fun createInfoSourceReference(client: Client, recommendation: Recommendation, url: String, title: String? = null,
                                       modifyValues: (Client, Recommendation) -> Unit = {client, recommendation -> Unit}): InfoSourceReference {

        // Security checks
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, recommendation.infoChannel),
                "Client not member of info channel with UUID: ${recommendation.infoChannel.uuid}")

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must be provided")

        // Find info source
        val infoSource = infoSourceService.getInfoSourceByUrl(url) ?: infoSourceService.createInfoSourceFromUrl(client, url)

        // Create info source reference
        val infoSourceReference = InfoSourceReference()
        infoSourceReference.uuid = CryptoUtil().uuid()
        infoSourceReference.url = url
        infoSourceReference.title = title
        infoSourceReference.creator = client
        infoSourceReference.recommendation = recommendation
        infoSourceReference.infoSource = infoSource

        // Glue it all together
        client.infoSourceReferences.add(infoSourceReference)
        recommendation.infoSourceReferences.add(infoSourceReference)
        infoSource.infoSourceReferences.add(infoSourceReference)

        // Save it all
        val modifiedInfoSourceReference = infoSourceReferenceRepository.save(infoSourceReference)
        val modifiedClient = clientRepository.save(client)
        infoSourceRepository.save(infoSource)
        val modifiedRecommendation = recommendationRepository.save(recommendation)

        modifyValues(modifiedClient, modifiedRecommendation)

        return modifiedInfoSourceReference
    }

    /**
     * Validates an url against the given info source.
     *
     * @param url         Url
     * @param infoSource  Info source
     * @throws            Exception if url does not validate
     */
    open fun validateUrlReference(url: String, infoSource: InfoSource) {
        // TODO! Implement domain validator!
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must not be empty")

        var didMatchAny = false
        for (domain in infoSource.domains) {
            didMatchAny = didMatchAny || url.toLowerCase().startsWith(domain.domain)
        }
        Assert.isTrue(
                didMatchAny,
                "URL must be from the given info source, e.g. it must start with fx. " + infoSource.domains.first().domain + ", but was: " + url
        )
    }
}
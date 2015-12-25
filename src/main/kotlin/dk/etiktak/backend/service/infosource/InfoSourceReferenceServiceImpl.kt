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

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
class InfoSourceReferenceServiceImpl @Autowired constructor(
        private val infoSourceReferenceRepository: InfoSourceReferenceRepository,
        private val infoSourceRepository: InfoSourceRepository,
        private val infoChannelRepository: InfoChannelRepository,
        private val clientRepository: ClientRepository) : InfoSourceReferenceService {

    private val logger = LoggerFactory.getLogger(InfoSourceReferenceServiceImpl::class.java)

    /**
     * Finds an info source reference from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info source reference with given UUID
     */
    override fun getInfoSourceReferenceByUuid(uuid: String): InfoSourceReference? {
        return infoSourceReferenceRepository.findByUuid(uuid)
    }

    /**
     * Creates an info source reference.
     *
     * @param client           Creator
     * @param infoChannel      Info channel to attach
     * @param infoSource       Info source from which it is based
     * @param url              Reference URL
     * @param title            Title of reference
     * @param summaryMarkdown  Summary markdown
     * @return                 Created info source reference
     */
    override fun createInfoSourceReference(client: Client, infoChannel: InfoChannel, infoSource: InfoSource,
                                           url: String, title: String, summaryMarkdown: String): InfoSourceReference {

        // Check for empty fields
        Assert.notNull(
                client,
                "Client must be provided")

        Assert.notNull(
                infoChannel,
                "Info channel must be provided")

        Assert.notNull(
                infoSource,
                "Info source must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(title),
                "Title must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(summaryMarkdown),
                "Summary markdown must be provided")

        // Validate url
        validateUrlReference(url, infoSource)

        logger.info("Creating new info source reference with title: $title")

        // Create info source
        val infoSourceReference = InfoSourceReference()
        infoSourceReference.uuid = CryptoUtil().uuid()
        infoSourceReference.url = url
        infoSourceReference.title = title
        infoSourceReference.summaryMarkdown = summaryMarkdown
        infoSourceReference.creator = client
        infoSourceReference.infoSource = infoSource
        infoSourceReference.infoChannel = infoChannel

        // Glue it all together
        client.infoSourceReferences.add(infoSourceReference)
        infoChannel.infoSourceReferences.add(infoSourceReference)
        infoSource.infoSourceReferences.add(infoSourceReference)

        // Save it all
        infoSourceReferenceRepository.save(infoSourceReference)
        clientRepository.save(client)
        infoSourceRepository.save(infoSource)
        infoChannelRepository.save(infoChannel)

        return infoSourceReference
    }

    /**
     * Validates an info source reference url.
     *
     * @param url         Url
     * @param infoSource  Info source
     * @throws            Exception if url does not validate
     */
    private fun validateUrlReference(url: String, infoSource: InfoSource) {
        // TODO! Implement url prefix validator!
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must not be empty")

        var didMatchAny = false
        for (urlPrefix in infoSource.urlPrefixes) {
            didMatchAny = didMatchAny || url.toLowerCase().startsWith(urlPrefix.urlPrefix)
        }
        Assert.isTrue(
                didMatchAny,
                "URL must be from the given info source, e.g. it must start with fx. " + infoSource.urlPrefixes.first() + ", but was: " + url
        )
    }
}
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
import dk.etiktak.backend.model.infosource.InfoSourceUrlPrefix
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceUrlPrefixRepository
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
@Transactional
class InfoSourceServiceImpl @Autowired constructor(
        private val infoSourceRepository: InfoSourceRepository,
        private val infoSourceUrlPrefixRepository: InfoSourceUrlPrefixRepository) : InfoSourceService {

    private val logger = LoggerFactory.getLogger(InfoSourceServiceImpl::class.java)

    /**
     * Finds an info source from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info source with given UUID
     */
    override fun getInfoSourceByUuid(uuid: String): InfoSource? {
        return infoSourceRepository.findByUuid(uuid)
    }

    /**
     * Creates an info source.
     *
     * @param client         Creator
     * @param urlPrefixes    Url prefixes of info source
     * @param friendlyName   Name of info source
     * @return               Created info source
     */
    override fun createInfoSource(client: Client, urlPrefixes: List<String>, friendlyName: String): InfoSource {

        // Validate url prefix
        for (urlPrefix in urlPrefixes) {
            validateUrlPrefix(urlPrefix)
        }

        logger.info("Creating new info source with name: $friendlyName")

        // Create info source
        val infoSource = InfoSource()
        infoSource.uuid = CryptoUtil().uuid()
        infoSource.friendlyName = friendlyName

        // Create url prefixes
        for (urlPrefix in urlPrefixes) {
            val infoSourceUrlPrefix = InfoSourceUrlPrefix()
            infoSourceUrlPrefix.uuid = CryptoUtil().uuid()
            infoSourceUrlPrefix.urlPrefix = urlPrefix
            infoSourceUrlPrefix.infoSource = infoSource

            infoSource.urlPrefixes.add(infoSourceUrlPrefix)
        }

        // Save it all
        infoSourceRepository.save(infoSource)
        for (infoSourceUrlPrefix in infoSource.urlPrefixes) {
            infoSourceUrlPrefixRepository.save(infoSourceUrlPrefix)
        }

        return infoSource
    }

    /**
     * Validates an info source url prefix.
     *
     * @param urlPrefix      Url prefix
     * @throws               Exception if url prefix does not validate
     */
    private fun validateUrlPrefix(urlPrefix: String) {
        // TODO! Implement url prefix validator!
        Assert.isTrue(
                !StringUtils.isEmpty(urlPrefix),
                "URL prefix must not be empty")

        Assert.isTrue(
                urlPrefix.toLowerCase().startsWith("http://") || urlPrefix.toLowerCase().startsWith("https://"),
                "URL prefix must start with http:// or https://. Got: " + urlPrefix
        )
    }
}
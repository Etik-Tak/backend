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

package dk.etiktak.backend.service.client

import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.trust.TrustService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class ClientService @Autowired constructor(
        private val clientRepository: ClientRepository,
        private val trustService: TrustService) {

    private val logger = LoggerFactory.getLogger(ClientService::class.java)

    /**
     * Creates a client entry. Throws exception if client with mobile number *and* given password already exists.
     *
     * @return  Created client entry
     */
    open fun createClient(): Client {

        // Create client
        var client = Client()
        client.uuid = CryptoUtil().uuid()
        client.mobileNumberHashPasswordHashHashed = null
        client.verified = false
        clientRepository.save(client)

        logger.info("Created new client with uuid: ${client.uuid}")

        // Update initial trust
        trustService.recalculateClientTrustLevel(client, modifyValues = {modifiedClient -> client = modifiedClient})

        return client
    }

    /**
     * Finds client by UUID.
     *
     * @param uuid    Client UUID
     * @return        Client
     */
    open fun getByUuid(uuid: String): Client? {
        return clientRepository.findByUuid(uuid)
    }
}
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

package dk.etiktak.backend.service.infochannel

import dk.etiktak.backend.model.acl.AclRole
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infochannel.InfoChannelClient
import dk.etiktak.backend.model.infochannel.InfoChannelFollower
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelClientRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelFollowerRepository
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InfoChannelServiceImpl @Autowired constructor(
        private val clientRepository: ClientRepository,
        private val infoChannelRepository: InfoChannelRepository,
        private val infoChannelClientRepository: InfoChannelClientRepository,
        private val infoChannelFollowerRepository: InfoChannelFollowerRepository) : InfoChannelService {

    private val logger = LoggerFactory.getLogger(InfoChannelServiceImpl::class.java)

    /**
     * Finds an info channel from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info channel with given UUID
     */
    override fun getInfoChannelByUuid(uuid: String): InfoChannel? {
        return infoChannelRepository.findByUuid(uuid)
    }

    /**
     * Creates an info channel with the given client as owner.
     *
     * @param client        Owner
     * @param name          Name of info channel
     * @param modifyValues  Function called with modified client
     * @return              Created info channel
     */
    override fun createInfoChannel(client: Client, name: String, modifyValues: (Client) -> Unit): InfoChannel {
        logger.info("Creating new info channel with name: $name")

        // Create info channel
        val infoChannel = InfoChannel()
        infoChannel.uuid = CryptoUtil().uuid()
        infoChannel.name = name

        // Create info channel client
        val infoChannelClient = InfoChannelClient()
        infoChannelClient.uuid = CryptoUtil().uuid()
        infoChannelClient.client = client
        infoChannelClient.infoChannel = infoChannel
        infoChannelClient.roles.add(AclRole.OWNER)

        // Glue them together
        infoChannel.infoChannelClients.add(infoChannelClient)
        client.infoChannelClients.add(infoChannelClient)

        // Save them all
        var modifiedClient = clientRepository.save(client)
        var modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        infoChannelClientRepository.save(infoChannelClient)

        modifiedClient = clientRepository.findByUuid(modifiedClient.uuid)

        // Follow ones own info channel
        followInfoChannel(modifiedClient, modifiedInfoChannel,
                modifyValues = {client, infoChannel -> modifiedClient = client; modifiedInfoChannel = infoChannel})

        modifyValues(modifiedClient)

        return modifiedInfoChannel
    }

    /**
     * Marks the client as following the given info channel.
     *
     * @param client        Client
     * @param infoChannel   Info channel
     * @param modifyValues  Function called with modified client and info channel
     */
    override fun followInfoChannel(client: Client, infoChannel: InfoChannel, modifyValues: (Client, InfoChannel) -> Unit) {

        // Create info channel client
        val infoChannelFollower = InfoChannelFollower()
        infoChannelFollower.uuid = CryptoUtil().uuid()
        infoChannelFollower.client = client
        infoChannelFollower.infoChannel = infoChannel

        // Glue them together
        infoChannel.followers.add(infoChannelFollower)
        client.followingInfoChannels.add(infoChannelFollower)

        // Save them all
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)
        infoChannelFollowerRepository.save(infoChannelFollower)

        modifyValues(modifiedClient, modifiedInfoChannel)
    }

    /**
     * Checks whether the given client is member of the given info channel.
     *
     * @param client        Client
     * @param infoChannel   Info channel
     * @return              True, if client is member of info channel, else false
     */
    override fun isClientMemberOfInfoChannel(client: Client, infoChannel: InfoChannel): Boolean {
        for (infoChannelClient in infoChannel.infoChannelClients) {
            if (infoChannelClient.client.uuid.equals(client.uuid)) {
                return true
            }
        }
        return false
    }
}

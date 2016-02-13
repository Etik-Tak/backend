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

import dk.etiktak.backend.model.acl.AclRole
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.model.user.ClientDevice
import dk.etiktak.backend.repository.user.ClientDeviceRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
@Transactional
open class ClientService @Autowired constructor(
        private val clientRepository: ClientRepository,
        private val clientDeviceRepository: ClientDeviceRepository,
        private val contributionService: ContributionService) {

    private val logger = LoggerFactory.getLogger(ClientService::class.java)

    /**
     * Creates a client entry. Either both of or none of username and password must be provided.
     *
     * @param deviceType   Device type
     * @param username     Optional username
     * @param password     Optional password
     * @return Created (non-hashed) device ID
     */
    open fun createClient(deviceType: ClientDevice.DeviceType = ClientDevice.DeviceType.Unknown,
                          username: String? = null, password: String? = null): String {

        // Ensure that either both of or none of username and password are provided
        Assert.isTrue(
                StringUtils.isEmpty(username) == StringUtils.isEmpty(password),
                "Either both of or none of username and password must be provided")

        // Create client
        var client = Client()
        client.uuid = CryptoUtil().uuid()
        client.verified = false
        client.role = AclRole.USER
        client.username = username
        client.passwordHashed = if (password != null) CryptoUtil().hash(password) else null

        clientRepository.save(client)

        // Create and attach device
        val deviceId = createDevice(client, deviceType, modifyValues = {modifiedClient -> client = modifiedClient})

        logger.info("Created new client with uuid: ${client.uuid}")

        // Update initial trust
        contributionService.recalculateClientTrustLevel(client, modifyValues = { modifiedClient -> client = modifiedClient})

        return deviceId
    }

    /**
     * Creates a new client device.
     *
     * @param inClient      Client
     * @param deviceType    Device type
     * @param modifyValues  Function called with modified client
     * @return              Created (non-hashed) device ID
     */
    @ClientValid
    open fun createDevice(inClient: Client, deviceType: ClientDevice.DeviceType? = ClientDevice.DeviceType.Unknown, modifyValues: (Client) -> Unit = {}): String {

        var client = inClient

        // Get random device ID
        val deviceId = CryptoUtil().uuid()

        // Create device
        var device = ClientDevice()
        device.uuid = CryptoUtil().uuid()
        device.idHashed = CryptoUtil().hash(deviceId)

        // Glue it together
        device.client = client
        client.devices.add(device)

        // Save it all
        client = clientRepository.save(client)
        clientDeviceRepository.save(device)

        modifyValues(client)

        return deviceId
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

    /**
     * Finds client by username and password.
     *
     * @param username   Username
     * @param password   Password
     * @return           Client
     */
    open fun getByUsernameAndPassword(username: String, password: String): Client? {
        return clientRepository.findByUsernameAndPasswordHashed(username, CryptoUtil().hash(password))
    }

    /**
     * Finds client by device ID.
     *
     * @param deviceId  Non-hashed device ID
     * @return          Client
     */
    open fun getByDeviceId(deviceId: String): Client? {
        return clientRepository.findByDevicesIdHashed(CryptoUtil().hash(deviceId))
    }
}

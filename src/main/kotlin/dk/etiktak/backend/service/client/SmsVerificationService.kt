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
import dk.etiktak.backend.model.user.MobileNumber
import dk.etiktak.backend.model.user.SmsVerification
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.repository.user.MobileNumberRepository
import dk.etiktak.backend.repository.user.SmsVerificationRepository
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
@Transactional
open class SmsVerificationService @Autowired constructor(
        private val clientRepository: ClientRepository,
        private val mobileNumberRepository: MobileNumberRepository,
        private val smsVerificationRepository: SmsVerificationRepository) {

    private val logger = LoggerFactory.getLogger(SmsVerificationService::class.java)

    /**
     * Creates and returns an SMS verification entry and sends the challenge part to the given mobile number.
     *
     * @param client          Client for which to attach
     * @param mobileNumber    Mobile number
     * @param modifyValues    Function called with modified client
     * @return                Created SMS verification
     */
    @ClientValid
    open fun requestSmsChallenge(client: Client, mobileNumber: String, recoveryEnabled: Boolean = false, modifyValues: (Client) -> Unit = {}): SmsVerification {

        val smsChallenge = CryptoUtil().generateSmsChallenge()
        val clientChallenge = CryptoUtil().uuid()

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided")

        logger.info("Requesting new SMS challenge for user with mobile number: $mobileNumber")

        // Fetch existing SMS verification, if any
        val smsVerification: SmsVerification?

        // Fetch existing mobile number, if any
        val mobile = mobileNumberRepository.findByMobileNumberHash(CryptoUtil().hash(mobileNumber))

        if (mobile != null) {

            // Mobile number already exists
            logger.info("Mobile number already exists: $mobileNumber")

            // Fetch existing SMS verification
            smsVerification = smsVerificationRepository.findByMobileNumberHash(CryptoUtil().hash(mobileNumber))

        } else {

            // New mobile number registration
            logger.info("New mobile number: $mobileNumber")

            // Create mobile number entry
            val mobileNumberEntity = createMobileNumber(mobileNumber)

            // Attach to client if recovery enabled
            if (recoveryEnabled) {
                client.mobileNumber = mobileNumberEntity
                mobileNumberEntity.client = client
                mobileNumberRepository.save(mobileNumberEntity)
            }

            // Create new SMS verification
            smsVerification = SmsVerification()
            smsVerification.mobileNumberHash = CryptoUtil().hash(mobileNumber)
        }

        // Check SMS verification
        Assert.notNull(
                smsVerification,
                "Internal error: SMS verification cannot be null while creating verification")

        smsVerification!!

        // Mark client as not verified
        client.verified = false
        client.smsChallengeHashClientChallengeHashHashed = CryptoUtil().hashOfHashes(smsChallenge, clientChallenge)
        val modifiedClient = clientRepository.save(client)

        // Set challenges on verification
        smsVerification.smsChallengeHash = CryptoUtil().hash(smsChallenge)
        smsVerification.clientChallenge = clientChallenge
        smsVerification.status = SmsVerification.SmsVerificationStatus.PENDING
        smsVerification.smsHandle = CryptoUtil().generateSmsHandle()
        smsVerificationRepository.save(smsVerification)

        // Send challenge
        logger.info("Sent SMS challenge $smsChallenge to mobile number $mobileNumber")
        logger.info("Sent client challenge $clientChallenge to mobile number $mobileNumber")

        smsVerification.status = SmsVerification.SmsVerificationStatus.SENT
        smsVerificationRepository.save(smsVerification)

        modifyValues(modifiedClient)

        return smsVerification
    }

    /**
     * Verifies a sent SMS challenge. Fails if client cannot be verified or SMS verification cannot be verified.
     *
     * @param client             Client
     * @param mobileNumber       Mobile number
     * @param smsChallenge       Received SMS challenge
     * @param clientChallenge    Received client challenge
     * @return                   Verified client
     */
    open fun verifySmsChallenge(client: Client, mobileNumber: String, smsChallenge: String, clientChallenge: String): Client {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(smsChallenge),
                "SMS challenge must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(clientChallenge),
                "Client challenge must be provided")

        logger.info("Verifying SMS challenge for mobile number: $mobileNumber")

        // Fetch SMS verification
        val smsVerification = smsVerificationRepository.findBySmsChallengeHashAndClientChallenge(
                CryptoUtil().hash(smsChallenge),
                clientChallenge)

        Assert.notNull(
                smsVerification,
                "SMS verification not found for mobile number $mobileNumber")

        smsVerification!!

        // Verify SMS challenge
        Assert.isTrue(
                smsVerification.status === SmsVerification.SmsVerificationStatus.SENT,
                "SMS verification has wrong status. Expected SENT but was '" + smsVerification.status.name + "'")

        Assert.isTrue(
                smsVerification.mobileNumberHash == CryptoUtil().hash(mobileNumber),
                "SMS verification does not belong to given mobile number: $mobileNumber")

        // Verify client
        val challengedClient = clientRepository.findBySmsChallengeHashClientChallengeHashHashed(
                CryptoUtil().hashOfHashes(smsChallenge, clientChallenge))

        Assert.notNull(
                challengedClient,
                "Client not found for mobile number $mobileNumber and given challenges")

        challengedClient!!

        Assert.isTrue(
                client.uuid == challengedClient.uuid,
                "Given client with UUID: ${client.uuid} not the one challenged!")

        // Change status of SMS verification
        smsVerification.status = SmsVerification.SmsVerificationStatus.VERIFIED
        smsVerificationRepository.save(smsVerification)

        // Mark client as verified
        client.verified = true
        client.smsChallengeHashClientChallengeHashHashed = null
        clientRepository.save(client)

        logger.info("SMS challenge verified successfully for mobile number: $mobileNumber")

        return client
    }

    /**
     * Creates and returns a recovery SMS verification and sends the challenge part to the given mobile number.
     *
     * @param mobileNumber    Mobile number
     * @return                Created SMS verification
     */
    open fun requestRecoverySmsChallenge(mobileNumber: String): SmsVerification {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided")

        logger.info("Requesting new recovery SMS challenge for user with mobile number: $mobileNumber")

        // Fetch client from mobile number (which can only be done if recovery is enabled)
        val client = clientRepository.findByMobileNumberMobileNumberHash(CryptoUtil().hash(mobileNumber))

        Assert.notNull(
                client,
                "Can only recover users with recovery enabled")

        client!!

        return requestSmsChallenge(client, mobileNumber)
    }

    /**
     * Creates a mobile number entry. Throws exception if mobile number already exists.
     *
     * @param mobileNumber    Mobile number
     * @return                Created mobile number entry
     */
    open fun createMobileNumber(mobileNumber: String): MobileNumber {
        Assert.isNull(
                mobileNumberRepository.findByMobileNumberHash(CryptoUtil().hash(mobileNumber)),
                "Mobile number $mobileNumber already exists")

        val number = MobileNumber()
        number.mobileNumberHash = CryptoUtil().hash(mobileNumber)
        mobileNumberRepository.save(number)
        return number
    }
}
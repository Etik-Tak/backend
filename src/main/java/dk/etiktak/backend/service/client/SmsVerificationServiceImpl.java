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

package dk.etiktak.backend.service.client;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.MobileNumber;
import dk.etiktak.backend.model.user.SmsVerification;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
import dk.etiktak.backend.util.CryptoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class SmsVerificationServiceImpl implements SmsVerificationService {
    private Logger logger = LoggerFactory.getLogger(SmsVerificationServiceImpl.class);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    /**
     * Creates and returns a recovery SMS verification entry and sends the challenge part to the given mobile number.
     *
     * @param mobileNumber    Mobile number
     * @param password        Password
     * @return                Created SMS verification
     * @throws Exception
     */
    @Override
    public SmsVerification requestRecoverySmsChallenge(String mobileNumber, String password) throws Exception {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided"
        );

        // Can only recover users with password
        Assert.isTrue(
                !StringUtils.isEmpty(password),
                "Can only recover users with password");

        // Fetch client entry
        Client client = clientRepository.findByMobileNumberHashPasswordHashHashed(
                CryptoUtil.hashOfHashes(mobileNumber, password));

        Assert.notNull(
                client,
                "Client not found for mobile number " + mobileNumber + " and given password");

        return requestSmsChallenge(client.getUuid(), mobileNumber, password);
    }

    /**
     * Creates and returns an SMS verification entry and sends the challenge part to the given mobile number.
     *
     * @param clientUuid      Client uuid for which to attach
     * @param mobileNumber    Mobile number
     * @param password        Choosen password
     * @return                Created SMS verification
     * @throws Exception
     */
    @Override
    public SmsVerification requestSmsChallenge(String clientUuid, String mobileNumber, String password) throws Exception {
        String smsChallenge = CryptoUtil.generateSmsChallenge();
        String clientChallenge = CryptoUtil.uuid();

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(clientUuid),
                "Client UUID must be provided"
        );

        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided"
        );

        Assert.isTrue(
                !StringUtils.isEmpty(password),
                "Password must be provided");

        // Fetch client from UUID
        Client client = clientRepository.findByUuid(clientUuid);
        Assert.notNull(
                client,
                "Client with UUID " + clientUuid + " does not exist");

        // Fetch existing SMS verification, if any
        SmsVerification smsVerification = smsVerificationRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber));

        // Fetch existing mobile number, if any
        MobileNumber mobile = mobileNumberRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber));

        if (mobile != null) {
            // Mobile number already exists

            // Check that mobile number and password for client is correct
            Assert.isTrue(
                    client.getMobileNumberHashPasswordHashHashed().equals(CryptoUtil.hashOfHashes(mobileNumber, password)),
                    "Mobile number " + mobileNumber + " already verified with other password than that provided"
            );
        } else {
            // New mobile number registration

            // Client with given mobile number and password cannot already exist
            Assert.isNull(
                    client.getMobileNumberHashPasswordHashHashed(),
                    "Internal error: Client with UUID " + clientUuid + " already verified, though mobile number entry " + mobileNumber + " did not exist");

            // SMS challenge cannot already exist
            Assert.isNull(
                    smsVerification,
                    "Internal error: SMS challenge already exists for mobile number " + mobileNumber + ", though mobile number entry did not exist");

            // Create mobile number entry
            createMobileNumber(mobileNumber);

            // Create new SMS verification
            smsVerification = new SmsVerification();
            smsVerification.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        }

        // Mark client as not verified
        client.setMobileNumberHashPasswordHashHashed(CryptoUtil.hashOfHashes(mobileNumber, password));
        client.setVerified(false);
        clientRepository.save(client);

        // Set challenges on verification
        smsVerification.setSmsChallengeHash(CryptoUtil.hash(smsChallenge));
        smsVerification.setClientChallenge(clientChallenge);
        smsVerification.setStatus(SmsVerification.SmsVerificationStatus.PENDING);
        smsVerification.setSmsHandle(CryptoUtil.generateSmsHandle());
        smsVerificationRepository.save(smsVerification);

        // Send challenge
        logger.info("Sent SMS challenge " + smsChallenge + " to mobile number " + mobileNumber);
        logger.info("Sent client challenge " + clientChallenge + " to mobile number " + mobileNumber);

        smsVerification.setStatus(SmsVerification.SmsVerificationStatus.SENT);
        smsVerificationRepository.save(smsVerification);

        return smsVerification;
    }

    /**
     * Verifies a sent SMS challenge. Fails if client cannot be verified or SMS verification cannot be verified.
     *
     * @param mobileNumber       Mobile number
     * @param password           Password
     * @param smsChallenge       Received SMS challenge
     * @param clientChallenge    Received client challenge
     * @return                   Verified client
     * @throws Exception
     */
    @Override
    public Client verifySmsChallenge(String mobileNumber, String password, String smsChallenge, String clientChallenge) throws Exception {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(mobileNumber),
                "Mobile number must be provided"
        );

        Assert.isTrue(
                !StringUtils.isEmpty(password),
                "Password must be provided");

        Assert.isTrue(
                !StringUtils.isEmpty(smsChallenge),
                "SMS challenge must be provided"
        );

        Assert.isTrue(
                !StringUtils.isEmpty(clientChallenge),
                "Client challenge must be provided"
        );

        // Verify mobile number and password
        Client client = clientRepository.findByMobileNumberHashPasswordHashHashed(
                CryptoUtil.hashOfHashes(mobileNumber, password));

        Assert.notNull(
                client,
                "Client not found for mobile number " + mobileNumber + " and given password");

        // Verify SMS challenge
        SmsVerification smsVerification = smsVerificationRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber));

        Assert.isTrue(
                smsVerification.getStatus() == SmsVerification.SmsVerificationStatus.SENT,
                "SMS verification has wrong status. Expected SENT but was '" + smsVerification.getStatus().name() + "'");
        Assert.isTrue(
                smsVerification.getSmsChallengeHash().equals(CryptoUtil.hash(smsChallenge)),
                "Provided SMS challenge does not match sent challenge");
        Assert.isTrue(
                smsVerification.getClientChallenge().equals(clientChallenge),
                "Provided client challenge does not match sent challenge");

        // Change status of SMS verification entry
        smsVerification.setStatus(SmsVerification.SmsVerificationStatus.VERIFIED);
        smsVerificationRepository.save(smsVerification);

        // Mark client as verified
        client.setVerified(true);
        clientRepository.save(client);

        return client;
    }

    /**
     * Creates a mobile number entry. Throws exception if mobile number already exists.
     *
     * @param mobileNumber    Mobile number
     * @return                Created mobile number entry
     * @throws Exception
     */
    private MobileNumber createMobileNumber(String mobileNumber) throws Exception {
        Assert.isNull(
                mobileNumberRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber)),
                "Mobile number " + mobileNumber + " already exists");

        MobileNumber number = new MobileNumber();
        number.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        mobileNumberRepository.save(number);
        return number;
    }
}

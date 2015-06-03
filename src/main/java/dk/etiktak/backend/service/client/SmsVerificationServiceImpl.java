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
     * Creates and returns an SMS verification entry and sends the challenge part to the given mobile number.
     *
     * @param clientUuid      Client uuid for which to attach
     * @param mobileNumber    Mobile number
     * @param password        Choosen password
     * @return                Created SMS verification
     * @throws Exception
     */
    @Override
    public SmsVerification sendSmsChallenge(String clientUuid, String mobileNumber, String password) throws Exception {
        String smsChallenge = CryptoUtil.generateSmsChallenge();
        String clientChallenge = CryptoUtil.uuid();

        // Fetch existing SMS verification, if any
        SmsVerification smsVerification = smsVerificationRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber));

        // Fetch existing client, if any
        Client client = clientRepository.findByMobileNumberHashPasswordHashHashed(CryptoUtil.getMobileNumberHashedPaswordHashedHashed(mobileNumber, password));

        // Fetch existing mobile number, if any
        MobileNumber mobile = mobileNumberRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber));

        if (mobile != null) {
            // Mobile number already exists

            // Client with given mobile number and password must already exist
            Assert.notNull(
                    client,
                    "Mobile number " + mobileNumber + " already verified, but client with given mobile number and password not found");

            // Mark client as not verified
            client.setVerified(false);
            clientRepository.save(client);

        } else {
            // New mobile number registration

            // Client with given mobile number and password cannot already exist
            Assert.isNull(
                    client,
                    "Internal error: Client with mobile number " + mobileNumber + " and given password already exists, though mobile number entry did not");

            // SMS challenge cannot already exist
            Assert.isNull(
                    smsVerification,
                    "Internal error: SMS challenge already exists for mobile number " + mobileNumber);

            // Create mobile number entry
            createMobileNumber(mobileNumber);

            // Create new SMS verification
            smsVerification = new SmsVerification();
            smsVerification.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        }

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
     *
     * @throws Exception
     */
    @Override
    public void verifySmsChallenge(String mobileNumber, String password, String smsChallenge, String clientChallenge) throws Exception {
        // Verify mobile number and password
        Client client = clientRepository.findByMobileNumberHashPasswordHashHashed(CryptoUtil.getMobileNumberHashedPaswordHashedHashed(mobileNumber, password));

        Assert.notNull(
                client,
                "Wrong mobile number or password provided (" + mobileNumber + "/###)");

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

package dk.etiktak.backend.service.user;

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
public class UserServiceImpl implements UserService {
    private Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    /**
     * Creates a complete user, i.e. creates the mobile number and client entry.
     * Throws exception if either mobile number or client already exists.
     *
     * @param mobileNumber    Mobile number
     * @param password        Password
     * @throws Exception
     */
    @Override
    public void createUser(String mobileNumber, String password) throws Exception {
        createMobileNumber(mobileNumber);
        createClient(mobileNumber, password);
    }

    /**
     * Creates a mobile number entry. Throws exception if mobile number already exists.
     *
     * @param mobileNumber    Mobile number
     * @return                Created mobile number entry
     * @throws Exception
     */
    @Override
    public MobileNumber createMobileNumber(String mobileNumber) throws Exception {
        Assert.isNull(
                mobileNumberRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber)),
                "Mobile number " + mobileNumber + " already exists");

        MobileNumber number = new MobileNumber();
        number.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        mobileNumberRepository.save(number);
        return number;
    }

    /**
     * Creates a client entry. Throws exception if client with mobile number *and* given password already exists.
     *
     * @param mobileNumber    Mobile number
     * @param password        Password
     * @return                Created client entry
     * @throws Exception
     */
    @Override
    public Client createClient(String mobileNumber, String password) throws Exception {
        Assert.isNull(
                clientRepository.findByMobileNumberHashPasswordHashHashed(CryptoUtil.getMobileNumberHashedPaswordHashedHashed(mobileNumber, password)),
                "Client with mobile number " + mobileNumber + " and password ### already exists");

        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setMobileNumberHashPasswordHashHashed(CryptoUtil.getMobileNumberHashedPaswordHashedHashed(mobileNumber, password));
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }

    /**
     * Creates and returns an SMS verification entry and sends the challenge part to the given mobile number.
     *
     * @param mobileNumber    Mobile number
     * @return                Created SMS verification
     * @throws Exception
     */
    @Override
    public SmsVerification sendSmsChallenge(String mobileNumber) throws Exception {
        Assert.isNull(
                smsVerificationRepository.findByMobileNumberHash(CryptoUtil.hash(mobileNumber)),
                "SMS challenge already exists for mobile number " + mobileNumber);

        String smsChallenge = CryptoUtil.generateSmsChallenge();
        String clientChallenge = CryptoUtil.uuid();

        SmsVerification smsVerification = new SmsVerification();
        smsVerification.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        smsVerification.setSmsChallengeHash(CryptoUtil.hash(smsChallenge));
        smsVerification.setClientChallenge(clientChallenge);
        smsVerification.setStatus(SmsVerification.SmsVerificationStatus.SENT);
        smsVerification.setSmsHandle(CryptoUtil.generateSmsHandle());
        smsVerificationRepository.save(smsVerification);

        logger.info("Sent SMS challenge " + smsChallenge + " to mobile number " + mobileNumber);
        logger.info("Sent client challenge " + clientChallenge + " to mobile number " + mobileNumber);

        return smsVerification;
    }

    /**
     * Verifies a sent SMS challenge. Fails if
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
}

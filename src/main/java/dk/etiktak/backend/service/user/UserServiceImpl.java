package dk.etiktak.backend.service.user;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.MobileNumber;
import dk.etiktak.backend.model.user.SmsVerification;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
import dk.etiktak.backend.util.CryptoUtil;
import dk.etiktak.backend.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    @Override
    public List<Client> findClients() {
        return Lists.asList(clientRepository.findAll());
    }

    @Override
    public MobileNumber createMobileNumber(String number) {
        MobileNumber mobileNumber = new MobileNumber();
        mobileNumber.setMobileNumberHash(CryptoUtil.hash(number));
        mobileNumberRepository.save(mobileNumber);
        return mobileNumber;
    }

    @Override
    public Client createClient(String mobileNumber, String password) {
        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setMobileNumberHash_passwordHash_hashed(
                CryptoUtil.hash(
                        CryptoUtil.hash(mobileNumber) + CryptoUtil.hash(password)
                )
        );
        clientRepository.save(client);
        return client;
    }

    @Override
    public void sendSmsChallenge(String mobileNumber) throws NoSuchAlgorithmException {
        SmsVerification smsVerification = new SmsVerification();
        smsVerification.setMobileNumberHash(CryptoUtil.hash(mobileNumber));
        smsVerification.setSmsChallengeHash(CryptoUtil.hash(CryptoUtil.generateSmsChallenge()));
        smsVerification.setClientChallenge(CryptoUtil.uuid());
        smsVerification.setStatus(SmsVerification.SmsVerificationStatus.PENDING);
        smsVerification.setSmsHandle(CryptoUtil.generateSmsHandle());
        smsVerificationRepository.save(smsVerification);
    }
}

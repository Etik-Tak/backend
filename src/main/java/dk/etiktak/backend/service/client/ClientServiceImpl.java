package dk.etiktak.backend.service.client;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.repository.user.MobileNumberRepository;
import dk.etiktak.backend.repository.user.SmsVerificationRepository;
import dk.etiktak.backend.util.CryptoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {
    private Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MobileNumberRepository mobileNumberRepository;

    @Autowired
    private SmsVerificationRepository smsVerificationRepository;

    /**
     * Creates a client entry. Throws exception if client with mobile number *and* given password already exists.
     *
     * @return                Created client entry
     * @throws Exception
     */
    @Override
    public Client createClient() throws Exception {
        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setMobileNumberHashPasswordHashHashed(null);
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }
}

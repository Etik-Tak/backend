package dk.etiktak.backend.service.user;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.MobileNumber;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface UserService {
    List<Client> findClients();

    MobileNumber createMobileNumber(String mobileNumber) throws Exception;
    Client createClient(String mobileNumber, String password) throws Exception;
    void sendSmsChallenge(String mobileNumber) throws Exception;
}

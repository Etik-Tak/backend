package dk.etiktak.backend.service.user;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.MobileNumber;
import dk.etiktak.backend.model.user.SmsVerification;

public interface UserService {
    void createUser(String username, String password) throws Exception;

    Client createClient(String mobileNumber, String password) throws Exception;

    MobileNumber createMobileNumber(String mobileNumber) throws Exception;

    SmsVerification sendSmsChallenge(String mobileNumber) throws Exception;
    void verifySmsChallenge(String mobileNumber, String password, String smsChallenge, String clientChallenge) throws Exception;
}

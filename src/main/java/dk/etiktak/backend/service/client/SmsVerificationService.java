package dk.etiktak.backend.service.client;

import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.model.user.SmsVerification;

public interface SmsVerificationService {
    SmsVerification sendRecoverySmsChallenge(String mobileNumber, String password) throws Exception;
    SmsVerification sendSmsChallenge(String clientUuid, String mobileNumber, String password) throws Exception;

    Client verifySmsChallenge(String mobileNumber, String password, String smsChallenge, String clientChallenge) throws Exception;
}

package dk.etiktak.backend.service.client;

import dk.etiktak.backend.model.user.SmsVerification;

public interface SmsVerificationService {
    SmsVerification sendSmsChallenge(String clientUuid, String mobileNumber, String password) throws Exception;
    void verifySmsChallenge(String mobileNumber, String password, String smsChallenge, String clientChallenge) throws Exception;
}

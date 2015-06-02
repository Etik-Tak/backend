package dk.etiktak.backend.controllers.rest.json;

import dk.etiktak.backend.model.user.SmsVerification;

public class SmsVerificationJsonObject extends BaseJsonObject {
    private String clientChallenge;

    public SmsVerificationJsonObject(SmsVerification smsVerification) {
        this.clientChallenge = smsVerification.getClientChallenge();
    }

    public String getClientChallenge() {
        return clientChallenge;
    }
}

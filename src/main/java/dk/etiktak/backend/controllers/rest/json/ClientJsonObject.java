package dk.etiktak.backend.controllers.rest.json;

import dk.etiktak.backend.model.user.Client;

public class ClientJsonObject extends BaseJsonObject {
    private String uuid;
    private Boolean verified;

    public ClientJsonObject(Client client) {
        this.uuid = client.getUuid();
        this.verified = client.getVerified();
    }

    public String getUuid() {
        return uuid;
    }

    public Boolean getVerified() {
        return verified;
    }
}

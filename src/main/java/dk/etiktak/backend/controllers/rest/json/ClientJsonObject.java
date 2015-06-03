package dk.etiktak.backend.controllers.rest.json;

import dk.etiktak.backend.model.user.Client;

public class ClientJsonObject extends BaseJsonObject {
    private String clientUuid;
    private Boolean verified;

    public ClientJsonObject(Client client) {
        this.clientUuid = client.getUuid();
        this.verified = client.getVerified();
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public Boolean getVerified() {
        return verified;
    }
}

/**
 * Client roles in relation to info channels.
 */
package dk.etiktak.backend.model.channel;

import dk.etiktak.backend.model.acl.Role;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("InfoChannelClient")
public class InfoChannelRole extends Role {

    @NotNull
    @ManyToOne(optional = true)
    @JoinColumn(name = "roles")
    private InfoChannelClient infoChannelClient;



    public InfoChannelRole() {
        super();
    }



    public InfoChannelClient getInfoChannelClient() {
        return infoChannelClient;
    }

    public void setInfoChannelClient(InfoChannelClient infoChannelClient) {
        this.infoChannelClient = infoChannelClient;
    }
}

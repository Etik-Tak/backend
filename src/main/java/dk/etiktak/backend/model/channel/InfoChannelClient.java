/**
 * Represents a specific list of roles for a client in relationship to a info channel.
 */

package dk.etiktak.backend.model.channel;

import dk.etiktak.backend.model.user.Client;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(name = "info_channel_clients")
public class InfoChannelClient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "info_channel_user_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "info_channel_id")
    private InfoChannel infoChannel;

    @OneToMany(mappedBy = "infoChannelClient", fetch = FetchType.LAZY)
    private List<InfoChannelRole> infoChannelRoles = new ArrayList<>();

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date creationTime;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date modificationTime;



    public InfoChannelClient() {}

    @PreUpdate
    public void preUpdate() {
        modificationTime = new Date();
    }

    @PrePersist
    public void prePersist() {
        Date now = new Date();
        creationTime = now;
        modificationTime = now;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public InfoChannel getInfoChannel() {
        return infoChannel;
    }

    public void setInfoChannel(InfoChannel infoChannel) {
        this.infoChannel = infoChannel;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public List<InfoChannelRole> getInfoChannelRoles() {
        return infoChannelRoles;
    }

    public void setInfoChannelRoles(List<InfoChannelRole> infoChannelRoles) {
        this.infoChannelRoles = infoChannelRoles;
    }
}

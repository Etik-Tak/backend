package dk.etiktak.backend.model.channel;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(name = "info_channels")
public class InfoChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "info_channel_id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @NotNull
    @OneToMany(mappedBy="infoChannel", fetch=FetchType.LAZY)
    private List<InfoChannelClient> infoChannelClients = new ArrayList<>();

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date creationTime;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date modificationTime;



    public InfoChannel() {}

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public List<InfoChannelClient> getInfoChannelClients() {
        return infoChannelClients;
    }

    public void setInfoChannelClients(List<InfoChannelClient> infoChannelClients) {
        this.infoChannelClients = infoChannelClients;
    }
}

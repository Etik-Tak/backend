/**
 * A client representation, i.e. a (mobile) user. Can be in two states:
 *
 * - Unverified; Object contains random uuid, has verified set to false and contains empty hash of mobile number and password
 * - Verified; Object contains random uuid, has verified set to true and contains a hash of mobile number and password
 *
 **/

package dk.etiktak.backend.model.user;

import dk.etiktak.backend.model.channel.InfoChannelClient;
import dk.etiktak.backend.model.product.ProductScan;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "mobileNumberHash_passwordHash_hashed", nullable = true, unique = true)
    private String mobileNumberHashPasswordHashHashed;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @NotNull
    @OneToMany(mappedBy="client", fetch=FetchType.LAZY)
    private List<ProductScan> productScans = new ArrayList<>();

    @OneToMany(mappedBy="client", fetch=FetchType.LAZY)
    private List<InfoChannelClient> infoChannelClients = new ArrayList<>();

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date creationTime;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date modificationTime;



    public Client() {}

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

    public String getMobileNumberHashPasswordHashHashed() {
        return mobileNumberHashPasswordHashHashed;
    }

    public void setMobileNumberHashPasswordHashHashed(String mobileNumberHashPasswordHashHashed) {
        this.mobileNumberHashPasswordHashHashed = mobileNumberHashPasswordHashHashed;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
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

    public List<ProductScan> getProductScans() {
        return productScans;
    }

    public void setProductScans(List<ProductScan> productScans) {
        this.productScans = productScans;
    }

    public List<InfoChannelClient> getInfoChannelClients() {
        return infoChannelClients;
    }

    public void setInfoChannelClients(List<InfoChannelClient> infoChannelClients) {
        this.infoChannelClients = infoChannelClients;
    }
}

package dk.etiktak.backend.model.user;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "mobileNumberHash_passwordHash_hashed", nullable = true, unique = true)
    private String mobileNumberHashPasswordHashHashed;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

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
}

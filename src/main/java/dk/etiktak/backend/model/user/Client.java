package dk.etiktak.backend.model.user;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "mobileNumberHash_passwordHash_hashed", nullable = false, unique = true)
    private String mobileNumberHash_passwordHash_hashed;

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

    public String getMobileNumberHash_passwordHash_hashed() {
        return mobileNumberHash_passwordHash_hashed;
    }

    public void setMobileNumberHash_passwordHash_hashed(String mobileNumberHash_passwordHash_hashed) {
        this.mobileNumberHash_passwordHash_hashed = mobileNumberHash_passwordHash_hashed;
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

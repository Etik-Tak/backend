/**
 * Used to keep track of which mobile numbers are in use. Since client entity knows nothing about
 * mobile number (without password) we need this entity. However, there is no direct relation between
 * a client and its mobile number.
 **/

package dk.etiktak.backend.model.user;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "mobile_number")
public class MobileNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "mobileNumberHash", nullable = false, unique = true)
    private String mobileNumberHash;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date creationTime;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date modificationTime;

    public MobileNumber() {}

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

    public String getMobileNumberHash() {
        return mobileNumberHash;
    }

    public void setMobileNumberHash(String mobileNumberHash) {
        this.mobileNumberHash = mobileNumberHash;
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

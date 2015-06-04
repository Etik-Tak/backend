// Copyright (c) 2015, Daniel Andersen (daniel@trollsahead.dk)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. The name of the author may not be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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

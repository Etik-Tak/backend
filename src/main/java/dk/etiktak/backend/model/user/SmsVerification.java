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
 * SMS verification entity keeps track of client verification. It consists of two parts; 1) the
 * actual SMS verification, which the user has to verify by receiving a SMS, and 2) a "hidden"
 * client challenge sent from the server to the client. Since the SMS verification challenge
 * is inheritedly weak a client challenge is used to strengthen security, thus forcing an
 * attacker to guess also a client challenge.
 */

package dk.etiktak.backend.model.user;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "sms_verifications")
public class SmsVerification {

    public enum SmsVerificationStatus { PENDING,  SENT,  FAILED, VERIFIED }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sms_verification_id")
    private Long id;

    @Column(name = "mobileNumberHash", nullable = false, unique = true)
    private String mobileNumberHash;

    @Column(name = "smsHandle")
    private String smsHandle;

    @Column(name = "smsChallengeHash", nullable = false)
    private String smsChallengeHash;

    @Column(name = "clientChallenge", nullable = false)
    private String clientChallenge;

    @Column(name = "status", nullable = false)
    private SmsVerificationStatus status;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date creationTime;

    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date modificationTime;



    public SmsVerification() {}

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

    public String getSmsHandle() {
        return smsHandle;
    }

    public void setSmsHandle(String smsHandle) {
        this.smsHandle = smsHandle;
    }

    public String getSmsChallengeHash() {
        return smsChallengeHash;
    }

    public void setSmsChallengeHash(String smsChallengeHash) {
        this.smsChallengeHash = smsChallengeHash;
    }

    public String getClientChallenge() {
        return clientChallenge;
    }

    public void setClientChallenge(String clientChallenge) {
        this.clientChallenge = clientChallenge;
    }

    public SmsVerificationStatus getStatus() {
        return status;
    }

    public void setStatus(SmsVerificationStatus status) {
        this.status = status;
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

// Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)
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
 * A client device; each client can have one or more devices attached.
 **/

package dk.etiktak.backend.model.user

import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity(name = "client_devices")
class ClientDevice {

    enum class DeviceType {
        Android,
        iOS,
        Unknown
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_device_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "id_hashed", nullable = false, unique = true)
    var idHashed: String = ""

    @Column(name = "type", nullable = false)
    var type = DeviceType.Unknown

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    var client = Client()

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var creationTime = Date()

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var modificationTime = Date()


    @PreUpdate
    fun preUpdate() {
        modificationTime = Date()
    }

    @PrePersist
    fun prePersist() {
        val now = Date()
        creationTime = now
        modificationTime = now
    }
}
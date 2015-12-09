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
 * Represents a specific list of roles for a client in relationship to a info channel.
 */

package dk.etiktak.backend.model.channel

import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity(name = "info_channel_clients")
class InfoChannelClient constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "info_channel_user_id")
    private var id: Long = 0

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private var client: Client = Client()

    @ManyToOne(optional = false)
    @JoinColumn(name = "info_channel_id")
    private var infoChannel: InfoChannel = InfoChannel()

    @OneToMany(mappedBy = "infoChannelClient", fetch = FetchType.LAZY)
    private var infoChannelRoles: MutableList<InfoChannelRole> = ArrayList()

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private var creationTime: Date = Date()

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private var modificationTime: Date = Date()



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
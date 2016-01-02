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

package dk.etiktak.backend.model.user

import dk.etiktak.backend.controller.rest.json.Jsonifier
import dk.etiktak.backend.controller.rest.json.JsonFilter
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infochannel.InfoChannelClient
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.model.recommendation.Recommendation
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "clients")
@Jsonifier(key = "client")
class Client constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_id")
    var id: Long = 0

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE, JsonFilter.CREATE))
    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "mobileNumberHash_passwordHash_hashed", nullable = true, unique = true)
    var mobileNumberHashPasswordHashHashed: String? = null

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE))
    @Column(name = "verified", nullable = false)
    var verified: Boolean = false

    @NotNull
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    var productScans: MutableList<ProductScan> = ArrayList()

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    var infoChannelClients: MutableList<InfoChannelClient> = ArrayList()

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE))
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    var followingInfoChannels: MutableList<InfoChannelClient> = ArrayList()

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    var infoSourceReferences: MutableList<InfoSourceReference> = ArrayList()

    @NotNull
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    var productCategories: MutableList<ProductCategory> = ArrayList()

    @NotNull
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    var productLabels: MutableList<ProductLabel> = ArrayList()

    @NotNull
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    var products: MutableList<Product> = ArrayList()

    @NotNull
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    var recommendations: MutableList<Recommendation> = ArrayList()

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE, JsonFilter.CREATE))
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var creationTime: Date = Date()

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE))
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var modificationTime: Date = Date()


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
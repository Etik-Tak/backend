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
 * Represents a product label.
 */

package dk.etiktak.backend.model.product

import dk.etiktak.backend.controller.rest.json.Jsonifier
import dk.etiktak.backend.controller.rest.json.JsonFilter
import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity(name = "product_labels")
@Jsonifier(key = "productLabel")
class ProductLabel constructor() : BaseModel() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "product_label_id")
    var id: Long = 0

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE, JsonFilter.CREATE))
    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE))
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    var creator: Client = Client()

    @Jsonifier(filter = arrayOf(JsonFilter.RETRIEVE, JsonFilter.CREATE))
    @Column(name = "name")
    var name: String = ""

    @ManyToMany(mappedBy = "productLabels", fetch = FetchType.LAZY)
    var products: MutableSet<Product> = HashSet()

    @ManyToMany(mappedBy = "productLabels", fetch = FetchType.LAZY)
    var infoSourceReferences: MutableSet<InfoSourceReference> = HashSet()

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
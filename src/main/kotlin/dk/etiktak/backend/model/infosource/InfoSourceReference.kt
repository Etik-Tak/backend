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
 * Represents a reference to an info source publication, including url and summary of content.
 */

package dk.etiktak.backend.model.infosource

import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "info_source_reference")
class InfoSourceReference constructor() : BaseModel() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "info_source_reference_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "url")
    var url: String = ""

    @Column(name = "title")
    var title: String = ""

    @Column(name = "summary")
    var summary: String = ""

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    var creator: Client = Client()

    @ManyToOne(optional = false)
    @JoinColumn(name = "info_channel_id")
    var infoChannel: InfoChannel = InfoChannel()

    @ManyToOne(optional = false)
    @JoinColumn(name = "info_source_id")
    var infoSource: InfoSource = InfoSource()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="infoSourceReference_product",
            joinColumns=arrayOf(JoinColumn(name="info_source_reference_id", referencedColumnName="info_source_reference_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="product_id", referencedColumnName="product_id")))
    @Column(name = "products")
    var products: MutableSet<Product> = HashSet()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="infoSourceReference_productCategory",
            joinColumns=arrayOf(JoinColumn(name="info_source_reference_id", referencedColumnName="info_source_reference_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="product_category_id", referencedColumnName="product_category_id")))
    @Column(name = "product_categories")
    var productCategories: MutableSet<ProductCategory> = HashSet()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="infoSourceReference_productLabel",
            joinColumns=arrayOf(JoinColumn(name="info_source_reference_id", referencedColumnName="info_source_reference_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="product_label_id", referencedColumnName="product_label_id")))
    @Column(name = "product_labels")
    var productLabels: MutableSet<ProductLabel> = HashSet()

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var creationTime: Date = Date()

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
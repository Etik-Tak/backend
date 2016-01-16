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
 * Represents a product.
 */

package dk.etiktak.backend.model.product

import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.recommendation.ProductRecommendation
import dk.etiktak.backend.model.trust.ProductTrustVote
import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "products")
class Product constructor() : BaseModel() {

    enum class BarcodeType {
        EAN13,
        UPC,
        UNKNOWN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "product_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "enabled")
    var enabled = true

    @Column(name = "barcode")
    var barcode: String = ""

    @Column(name = "barcode_type")
    var barcodeType = BarcodeType.UNKNOWN

    @Column(name = "name")
    var name: String = ""

    @Column(name = "correctness_trust")
    var correctnessTrust: Double = 0.0

    @Column(name = "initial_correctness_trust")
    var initialCorrectnessTrust: Double = 0.0

    @NotNull
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    var correctnessTrustVotes: MutableList<ProductTrustVote> = ArrayList()

    @OneToOne(cascade = arrayOf(CascadeType.ALL))
    var productHistory: Product? = null

    @NotNull
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    var productScans: MutableList<ProductScan> = ArrayList()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="product_productCategory",
            joinColumns=arrayOf(JoinColumn(name="product_id", referencedColumnName="product_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="product_category_id", referencedColumnName="product_category_id")))
    @Column(name = "product_categories")
    var productCategories: MutableSet<ProductCategory> = HashSet()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="product_productLabel",
            joinColumns=arrayOf(JoinColumn(name="product_id", referencedColumnName="product_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="product_label_id", referencedColumnName="product_label_id")))
    @Column(name = "product_labels")
    var productLabels: MutableSet<ProductLabel> = HashSet()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="product_company",
            joinColumns=arrayOf(JoinColumn(name="product_id", referencedColumnName="product_id")),
            inverseJoinColumns=arrayOf(JoinColumn(name="company_id", referencedColumnName="company_id")))
    @Column(name = "companies")
    var companies: MutableSet<Company> = HashSet()

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    var recommendations: MutableList<ProductRecommendation> = ArrayList()

    @ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
    var infoSourceReferences: MutableSet<InfoSourceReference> = HashSet()

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    var creator = Client()

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
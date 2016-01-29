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
 * Represents a company.
 */

package dk.etiktak.backend.model.company

import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.contribution.CompanyContribution
import dk.etiktak.backend.model.contribution.ProductCompanyContribution
import dk.etiktak.backend.model.contribution.StoreCompanyContribution
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.recommendation.CompanyRecommendation
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "companies")
class Company constructor() : BaseModel() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "company_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "name")
    var name: String = ""

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    var recommendations: MutableList<CompanyRecommendation> = ArrayList()

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    var stores: MutableList<Store> = ArrayList()

    @ManyToMany(mappedBy = "companies", fetch = FetchType.LAZY)
    var products: MutableSet<Product> = HashSet()

    @NotNull
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    var contributions: MutableList<CompanyContribution> = ArrayList()

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    var productContributions: MutableList<ProductCompanyContribution> = ArrayList()

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    var storeContributions: MutableList<StoreCompanyContribution> = ArrayList()

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
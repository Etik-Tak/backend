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
 * Represents a contribution, e.g. product name or a product category.
 */

package dk.etiktak.backend.model.contribution

import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "contributions")
open class Contribution : BaseModel() {

    enum class ContributionType {
        EditProductName,
        EditProductCategoryName,
        EditProductLabelName,
        AssignCategoryToProduct,
        AssignLabelToProduct,
        AssignTagToProduct,
        AssignCompanyToProduct,
        RemoveCompanyFromProduct,
        EditCompanyName,
        EditStoreName,
        AssignCompanyToStore,
        EditInfoSourceName,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "product_contribution_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    var client = Client()

    @Column(name = "type")
    var type = ContributionType.EditCompanyName

    @Column(name = "subject_uuid", nullable = false)
    var subjectUuid: String = ""

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @NotNull
    @OneToMany(mappedBy = "contribution", fetch = FetchType.LAZY)
    var trustVotes: MutableList<TrustVote> = ArrayList()

    @Column(name = "trust_score")
    var trustScore: Double = 0.0

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
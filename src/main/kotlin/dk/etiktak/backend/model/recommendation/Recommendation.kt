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
 * Represents a recommendation.
 */

package dk.etiktak.backend.model.recommendation

import dk.etiktak.backend.model.BaseModel
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.user.Client
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity(name = "recommendations")
@Table(uniqueConstraints = arrayOf(
        UniqueConstraint(columnNames = arrayOf("product_id", "info_channel_id")),
        UniqueConstraint(columnNames = arrayOf("product_category_id", "info_channel_id")),
        UniqueConstraint(columnNames = arrayOf("product_label_id", "info_channel_id")),
        UniqueConstraint(columnNames = arrayOf("product_tag_id", "info_channel_id"))))
open class Recommendation : BaseModel() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "recommendation_id")
    var id: Long = 0

    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String = ""

    @Column(name = "title")
    var title: String = ""

    @Column(name = "summary", columnDefinition="TEXT")
    var summary: String = ""

    @Column(name = "score")
    var score = RecommendationScore.NEUTRAL

    @NotNull
    @OneToMany(mappedBy = "recommendation", fetch = FetchType.LAZY)
    var infoSourceReferences: MutableList<InfoSourceReference> = ArrayList()

    @ManyToOne(optional = false)
    @JoinColumn(name = "info_channel_id")
    var infoChannel = InfoChannel()

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

enum class RecommendationScore {
    THUMBS_UP,
    NEUTRAL,
    THUMBS_DOWN
}

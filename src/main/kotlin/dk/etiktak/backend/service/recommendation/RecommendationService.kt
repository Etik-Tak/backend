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

package dk.etiktak.backend.service.recommendation

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.recommendation.RecommendationScore
import dk.etiktak.backend.model.user.Client

interface RecommendationService {

    fun getRecommendations(client: Client, product: Product): List<Recommendation>

    fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, product: Product,
                             modifyValues: (Client, InfoChannel) -> Unit = {client, infoChannel -> Unit}): Recommendation

    fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productCategory: ProductCategory,
                             modifyValues: (Client, InfoChannel) -> Unit = {client, infoChannel -> Unit}): Recommendation

    fun createRecommendation(client: Client, infoChannel: InfoChannel, summary: String, score: RecommendationScore, productLabel: ProductLabel,
                             modifyValues: (Client, InfoChannel) -> Unit = {client, infoChannel -> Unit}): Recommendation
}
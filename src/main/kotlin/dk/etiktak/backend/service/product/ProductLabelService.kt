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

package dk.etiktak.backend.service.product

import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.contribution.TextContribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.product.ProductLabelRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class ProductLabelService @Autowired constructor(
        private val productLabelRepository: ProductLabelRepository,
        private val contributionService: ContributionService) {

    private val logger = LoggerFactory.getLogger(ProductLabelService::class.java)

    /**
     * Finds a product label from the given UUID.
     *
     * @param uuid  UUID
     * @return      Product label with given UUID
     */
    open fun getProductLabelByUuid(uuid: String): ProductLabel? {
        return productLabelRepository.findByUuid(uuid)
    }

    /**
     * Creates a product label.
     *
     * @param inClient      Client
     * @param name          Name
     * @param modifyValues  Function called with modified client
     * @return              Product label
     */
    @ClientVerified
    open fun createProductLabel(inClient: Client, name: String, modifyValues: (Client) -> Unit = {}): ProductLabel {
        var client = inClient

        // Create product label
        var productLabel = ProductLabel()
        productLabel.uuid = CryptoUtil().uuid()
        productLabel.name = name

        productLabel = productLabelRepository.save(productLabel)

        // Create name contribution
        editProductLabelName(client, productLabel, name, modifyValues = {modifiedClient, modifiedProductLabel -> client = modifiedClient; productLabel = modifiedProductLabel})

        modifyValues(client)

        return productLabel
    }

    /**
     * Edits a product label name.
     *
     * @param inClient        Client
     * @param inProductLabel  Product label
     * @param name            Name of product label
     * @param modifyValues    Function called with modified client and product label
     * @return                Product label name contribution
     */
    @ClientVerified
    open fun editProductLabelName(inClient: Client, inProductLabel: ProductLabel, name: String, modifyValues: (Client, ProductLabel) -> Unit = {client, productLabel -> Unit}): Contribution {

        var client = inClient
        var productLabel = inProductLabel

        // Create contribution
        val contribution = contributionService.createTextContribution(Contribution.ContributionType.EditProductLabelName, client, productLabel.uuid, name, modifyValues = { modifiedClient -> client = modifiedClient})

        // Edit name
        productLabel.name = name
        productLabel = productLabelRepository.save(productLabel)

        modifyValues(client, productLabel)

        return contribution
    }

    /**
     * Returns whether the given client can edit the name of the product label.
     *
     * @param client        Client
     * @param productLabel  Product label
     * @return              Yes, if the given client can edit the name of the product label, or else false
     */
    open fun canEditProductLabelName(client: Client, productLabel: ProductLabel): Boolean {
        return client.verified && contributionService.hasSufficientTrustToEditContribution(client, productLabelNameContribution(productLabel))
    }

    /**
     * Returns the product label name contribution which is currently active.
     *
     * @param productLabel   Product label
     * @return               Product label name contribution
     */
    open fun productLabelNameContribution(productLabel: ProductLabel): TextContribution? {
        return contributionService.currentTextContribution(Contribution.ContributionType.EditProductLabelName, productLabel.uuid)
    }

    /**
     * Trust votes product label name.
     *
     * @param client            Client
     * @param productLabel      Product label
     * @param vote              Vote
     * @param modifyValues      Function called with modified client
     * @return                  Trust vote
     */
    open fun trustVoteProductLabelName(client: Client, productLabel: ProductLabel, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = productLabelNameContribution(productLabel)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }
}

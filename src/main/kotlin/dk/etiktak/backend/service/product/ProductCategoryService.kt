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

package dk.etiktak.backend.service.product

import dk.etiktak.backend.model.contribution.ProductCategoryNameContribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.contribution.ProductCategoryNameContributionRepository
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class ProductCategoryService @Autowired constructor(
        private val productCategoryRepository: ProductCategoryRepository,
        private val clientRepository: ClientRepository,
        private val productCategoryNameContributionRepository: ProductCategoryNameContributionRepository,
        private val contributionService: ContributionService) {

    private val logger = LoggerFactory.getLogger(ProductCategoryService::class.java)

    /**
     * Finds a product category from the given UUID.
     *
     * @param uuid  UUID
     * @return      Product category with given UUID
     */
    open fun getProductCategoryByUuid(uuid: String): ProductCategory? {
        return productCategoryRepository.findByUuid(uuid)
    }

    /**
     * Creates a product category.
     *
     * @param inClient      Client
     * @param name          Name
     * @param modifyValues  Function called with modified client
     * @return              Product category
     */
    @ClientVerified
    open fun createProductCategory(inClient: Client, name: String, modifyValues: (Client) -> Unit = {}): ProductCategory {
        var client = inClient

        // Create product category
        var productCategory = ProductCategory()
        productCategory.uuid = CryptoUtil().uuid()
        productCategory.name = name

        productCategory = productCategoryRepository.save(productCategory)

        // Create name contribution
        editProductCategoryName(client, productCategory, name,
                modifyValues = {modifiedClient, modifiedProductCategory -> client = modifiedClient; productCategory = modifiedProductCategory})

        modifyValues(client)

        return productCategory
    }

    /**
     * Edits a product category name.
     *
     * @param inClient           Client
     * @param inProductCategory  Product category
     * @param name               Name of product category
     * @param modifyValues       Function called with modified client and product category
     * @return                   Product category name contribution
     */
    @ClientVerified
    open fun editProductCategoryName(inClient: Client, inProductCategory: ProductCategory, name: String, modifyValues: (Client, ProductCategory) -> Unit = { client, productCategory -> Unit}): ProductCategoryNameContribution {

        var client = inClient
        var productCategory = inProductCategory

        // Get current name contribution
        val contributions = productCategoryNameContributionRepository.findByProductCategoryUuidAndEnabled(productCategory.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Check sufficient trust
            contributionService.assertSufficientTrustToEditContribution(client, currentContribution)

            // Disable current contribution
            currentContribution.enabled = false
            productCategoryNameContributionRepository.save(currentContribution)
        }

        // Edit name
        productCategory.name = name

        // Create name contribution
        var productCategoryNameContribution = ProductCategoryNameContribution()
        productCategoryNameContribution.uuid = CryptoUtil().uuid()
        productCategoryNameContribution.client = client
        productCategoryNameContribution.productCategory = productCategory
        productCategoryNameContribution.name = name

        // Glue it together
        productCategory.contributions.add(productCategoryNameContribution)
        client.contributions.add(productCategoryNameContribution)

        // Save it all
        client = clientRepository.save(client)
        productCategory = productCategoryRepository.save(productCategory)
        productCategoryNameContribution = productCategoryNameContributionRepository.save(productCategoryNameContribution)

        // Update trust
        contributionService.updateTrust(productCategoryNameContribution)

        modifyValues(client, productCategory)

        return productCategoryNameContribution
    }

    /**
     * Returns whether the given client can edit the name of the product category.
     *
     * @param client           Client
     * @param productCategory  Product category
     * @return                 Yes, if the given client can edit the name of the product category, or else false
     */
    open fun canEditProductCategoryName(client: Client, productCategory: ProductCategory): Boolean {
        return contributionService.hasSufficientTrustToEditContribution(client, productCategoryNameContribution(productCategory))
    }

    /**
     * Returns the product category name contribution which is currently active.
     *
     * @param productCategory   Product category
     * @return                  Product category name contribution
     */
    open fun productCategoryNameContribution(productCategory: ProductCategory): ProductCategoryNameContribution? {
        val contributions = productCategoryNameContributionRepository.findByProductCategoryUuidAndEnabled(productCategory.uuid)
        return contributionService.uniqueContribution(contributions)
    }

    /**
     * Trust votes product category name.
     *
     * @param client            Client
     * @param productCategory   Product category
     * @param vote              Vote
     * @param modifyValues      Function called with modified client
     * @return                  Trust vote
     */
    open fun trustVoteProductCategoryName(client: Client, productCategory: ProductCategory, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = productCategoryNameContribution(productCategory)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }
}

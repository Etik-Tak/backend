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

package dk.etiktak.backend.service.company

import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.company.Store
import dk.etiktak.backend.model.contribution.StoreCompanyContribution
import dk.etiktak.backend.model.contribution.StoreNameContribution
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.StoreRepository
import dk.etiktak.backend.repository.contribution.StoreCompanyContributionRepository
import dk.etiktak.backend.repository.contribution.StoreNameContributionRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
@Transactional
open class StoreService @Autowired constructor(
        private val storeRepository: StoreRepository,
        private val clientRepository: ClientRepository,
        private val contributionService: ContributionService,
        private val storeNameContributionRepository: StoreNameContributionRepository,
        private val storeCompanyContributionRepository: StoreCompanyContributionRepository) {

    private val logger = LoggerFactory.getLogger(StoreService::class.java)

    /**
     * Finds a store from the given UUID.
     *
     * @param uuid  UUID
     * @return      Store with given UUID
     */
    open fun getStoreByUuid(uuid: String): Store? {
        return storeRepository.findByUuid(uuid)
    }

    /**
     * Creates a new store.
     *
     * @param inClient      Client
     * @param name          Name of store
     * @param modifyValues  Function called with modified client
     * @return              Created store
     */
    @ClientVerified
    open fun createStore(inClient: Client, name: String, modifyValues: (Client) -> Unit = {}): Store {

        var client = inClient

        // Create store
        var store = Store()
        store.uuid = CryptoUtil().uuid()

        store = storeRepository.save(store)

        // Create name contribution
        editStoreName(client, store, name,
                modifyValues = {modifiedClient, modifiedStore -> client = modifiedClient; store = modifiedStore})

        modifyValues(client)

        return store
    }

    /**
     * Edits a store name.
     *
     * @param inClient        Client
     * @param inStore         Store
     * @param name            Name of store
     * @param modifyValues    Function called with modified client and store
     * @return                Store name contribution
     */
    @ClientVerified
    open fun editStoreName(inClient: Client, inStore: Store, name: String, modifyValues: (Client, Store) -> Unit = {client, store -> Unit}): StoreNameContribution {

        var client = inClient
        var store = inStore

        // Get current name contribution
        val contributions = storeNameContributionRepository.findByStoreUuidAndEnabled(store.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Check sufficient trust
            contributionService.assertSufficientTrustToEditContribution(client, currentContribution)

            // Disable current contribution
            currentContribution.enabled = false
            storeNameContributionRepository.save(currentContribution)
        }

        // Create name contribution
        var storeNameContribution = StoreNameContribution()
        storeNameContribution.uuid = CryptoUtil().uuid()
        storeNameContribution.client = client
        storeNameContribution.name = name

        // Glue it together
        store.contributions.add(storeNameContribution)
        client.contributions.add(storeNameContribution)

        // Save it all
        client = clientRepository.save(client)
        store = storeRepository.save(store)
        storeNameContribution = storeNameContributionRepository.save(storeNameContribution)

        // Update trust
        contributionService.updateTrust(storeNameContribution)

        modifyValues(client, store)

        return storeNameContribution
    }

    /**
     * Assigns a company to a store.
     *
     * @param inClient            Client
     * @param inStore             Store
     * @param inCompany           Company
     * @param modifyValues        Function called with modified client, product and company
     * @return                    Product company contribution
     */
    @ClientVerified
    open fun assignCompanyToProduct(inClient: Client, inStore: Store, inCompany: Company, modifyValues: (Client, Store, Company) -> Unit = { client, store, company -> Unit}): StoreCompanyContribution {

        var client = inClient
        var store = inStore
        var company = inCompany

        // Get current company contribution
        val contributions = storeCompanyContributionRepository.findByStoreUuidAndEnabled(store.uuid)
        val currentContribution = contributionService.uniqueContribution(contributions)

        currentContribution?.let {

            // Make sure it's disabled
            Assert.isTrue(
                    !currentContribution.enabled,
                    "Cannot assign company with UUID ${currentContribution.uuid} to store with UUID ${store.uuid}; it's already there and enabled!"
            )
        }

        // Create company contribution
        var storeCompanyContribution = StoreCompanyContribution()
        storeCompanyContribution.uuid = CryptoUtil().uuid()
        storeCompanyContribution.client = client
        storeCompanyContribution.company = company

        // Glue it together
        //company.productContributions.add(productCompanyContribution)
        store.contributions.add(storeCompanyContribution)
        client.contributions.add(storeCompanyContribution)

        // Save it all
        //company = companyRepository.save(company)
        client = clientRepository.save(client)
        store = storeRepository.save(store)
        storeCompanyContribution = storeCompanyContributionRepository.save(storeCompanyContribution)

        // Update trust
        contributionService.updateTrust(storeCompanyContribution)

        modifyValues(client, store, company)

        return storeCompanyContribution
    }

    /**
     * Returns the name of a store.
     *
     * @param store     Store
     * @return          Name of store
     */
    open fun storeName(store: Store): String? {
        val contributions = storeNameContributionRepository.findByStoreUuidAndEnabled(store.uuid)
        return contributionService.uniqueContribution(contributions)?.name
    }

    /**
     * Returns the company of a store.
     *
     * @param store     Store
     * @return          Company
     */
    open fun storeCompany(store: Store): Company? {
        val contributions = storeCompanyContributionRepository.findByStoreUuidAndEnabled(store.uuid)
        return contributionService.uniqueContribution(contributions)?.company
    }
}
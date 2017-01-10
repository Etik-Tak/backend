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
import dk.etiktak.backend.model.contribution.*
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.StoreRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class StoreService @Autowired constructor(
        private val storeRepository: StoreRepository,
        private val contributionService: ContributionService) {

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
        editStoreName(client, store, name, modifyValues = {modifiedClient, modifiedStore -> client = modifiedClient; store = modifiedStore})

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
    open fun editStoreName(inClient: Client, inStore: Store, name: String, modifyValues: (Client, Store) -> Unit = {client, store -> Unit}): Contribution {

        var client = inClient
        var store = inStore

        // Create contribution
        val contribution = contributionService.createTextContribution(Contribution.ContributionType.StoreName, client, store.uuid, name, modifyValues = {modifiedClient -> client = modifiedClient})

        // Edit name
        store.name = name
        store = storeRepository.save(store)

        modifyValues(client, store)

        return contribution
    }

    /**
     * Assigns a company to a store.
     *
     * @param client            Client
     * @param store             Store
     * @param company           Company
     * @param modifyValues      Function called with modified client
     * @return                  Product company contribution
     */
    @ClientVerified
    open fun assignCompanyToStore(client: Client, store: Store, company: Company, modifyValues: (Client) -> Unit = {}): Contribution {
        return contributionService.createReferenceContribution(Contribution.ContributionType.StoreCompany, client, store.uuid, company.uuid, modifyValues = modifyValues)
    }

    /**
     * Returns the store name contribution which is currently active.
     *
     * @param store     Store
     * @return          Store name contribution
     */
    open fun storeNameContribution(store: Store): TextContribution? {
        return contributionService.currentTextContribution(Contribution.ContributionType.StoreName, store.uuid)
    }

    /**
     * Returns whether the given client can edit the name of the store.
     *
     * @param client    Client
     * @param store     Store
     * @return          Yes,if the given client can edit the name of the store, or else false
     */
    open fun canEditStoreName(client: Client, store: Store): Boolean {
        return client.verified && contributionService.hasSufficientTrustToEditContribution(client, storeNameContribution(store))
    }

    /**
     * Trust votes store name.
     *
     * @param client          Client
     * @param store           Store
     * @param vote            Vote
     * @param modifyValues    Function called with modified client
     * @return                Trust vote
     */
    open fun trustVoteStoreName(client: Client, store: Store, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = storeNameContribution(store)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }
}
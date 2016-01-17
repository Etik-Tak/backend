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
import dk.etiktak.backend.model.location.Location
import dk.etiktak.backend.model.trust.TrustItem
import dk.etiktak.backend.model.trust.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.company.StoreRepository
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.trust.TrustItemRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.TrustService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class StoreService @Autowired constructor(
        private val companyRepository: CompanyRepository,
        private val storeRepository: StoreRepository,
        private val locationRepository: LocationRepository,
        private val clientRepository: ClientRepository,
        private val trustItemRepository: TrustItemRepository,
        private val trustService: TrustService) {

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
     * @param client        Client
     * @param name          Name of store
     * @param latitude      Latitude
     * @param longitude     Longitude
     * @param company       Company
     * @param modifyValues  Function called with modified client and company
     * @return              Created store
     */
    @ClientVerified
    open fun createStore(client: Client, name: String, latitude: Double, longitude: Double, company: Company, modifyValues: (Client, Company) -> Unit = {client, company -> Unit}): Store {

        // Create location
        val location = locationRepository.save(Location(latitude, longitude))
        locationRepository.save(location)

        // Create store
        val store = Store()
        store.company = company
        store.uuid = CryptoUtil().uuid()
        store.name = name
        store.location = location
        // TODO! Creator

        // Create trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Store, store.uuid, modifyValues = {client -> modifiedClient = client})
        store.trustItemUuid = trustItem.uuid

        // Glue it together
        company.stores.add(store)

        // Save it all
        var modifiedCompany = companyRepository.save(company)
        var modifiedStore = storeRepository.save(store)

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient, modifiedCompany)

        return modifiedStore
    }

    /**
     * Edits a store.
     *
     * @param client        Client
     * @param store         Store
     * @param name          Name of store
     * @param latitude      Store latitude
     * @param longitude     Store longitude
     * @param modifyValues  Function called with modified client and store
     */
    @ClientVerified
    open fun editStore(client: Client, store: Store, name: String?, latitude: Double?, longitude: Double?, modifyValues: (Client, Store) -> Unit = {client, store -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, storeTrustItem(store))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Store, store.uuid, modifyValues = {client -> modifiedClient = client})
        store.trustItemUuid = trustItem.uuid

        // Modify values
        store.name = name ?: store.name

        val oldLocation = store.location
        val location = if (latitude != null && longitude != null) Location(latitude, longitude) else null
        if (location != null) {
            store.location = location
        }

        // Save it all
        var modifiedStore = storeRepository.save(store)
        if (location != null) {
            locationRepository.save(location)
            locationRepository.delete(oldLocation)
        }

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient, modifiedStore)
    }

    /**
     * Trust vote store.
     *
     * @param client        Client
     * @param company       Store
     * @param vote          Vote
     * @param modifyValues  Function called with modified client and store
     * @return              Trust vote
     */
    @ClientValid
    open fun trustVoteStore(client: Client, store: Store, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        return trustService.trustVoteItem(client, storeTrustItem(store), vote,
                modifyValues = {modifiedClient, trustItem -> modifyValues(modifiedClient)})
    }

    /**
     * Returns the trust item of the given store.
     *
     * @param store     Store
     * @return          Trust item
     */
    open fun storeTrustItem(store: Store): TrustItem {
        return trustItemRepository.findByUuid(store.trustItemUuid)!!
    }
}
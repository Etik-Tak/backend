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
import dk.etiktak.backend.model.trust.TrustItem
import dk.etiktak.backend.model.trust.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.trust.TrustItemRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.TrustService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class CompanyService @Autowired constructor(
        private val companyRepository: CompanyRepository,
        private val clientRepository: ClientRepository,
        private val trustItemRepository: TrustItemRepository,
        private val trustService: TrustService) {

    private val logger = LoggerFactory.getLogger(CompanyService::class.java)

    /**
     * Finds a company from the given UUID.
     *
     * @param uuid  UUID
     * @return      Company with given UUID
     */
    open fun getCompanyByUuid(uuid: String): Company? {
        return companyRepository.findByUuid(uuid)
    }

    /**
     * Creates a new product.
     *
     * @param client        Client
     * @param name          Name of product
     * @param modifyValues  Function called with modified client
     * @return              Created product
     */
    @ClientVerified
    open fun createCompany(client: Client, name: String?, modifyValues: (Client) -> Unit = {}): Company {

        // Create company
        val company = Company()
        company.uuid = CryptoUtil().uuid()
        company.name = name ?: ""
        // TODO! Creator

        // Create trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Company, company.uuid, modifyValues = {client -> modifiedClient = client})
        company.trustItemUuid = trustItem.uuid

        // Save it all
        var modifiedCompany = companyRepository.save(company)

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient)

        return modifiedCompany
    }

    /**
     * Edits a company.
     *
     * @param client        Client
     * @param company       Company
     * @param name          Name of company
     * @param modifyValues  Function called with modified client and company
     */
    @ClientVerified
    open fun editCompany(client: Client, company: Company, name: String?, modifyValues: (Client, Company) -> Unit = {client, company -> Unit}) {

        // Check sufficient trust
        trustService.assertSufficientTrustToEditTrustItem(client, companyTrustItem(company))

        // Create new trust item
        var modifiedClient = client
        val trustItem = trustService.createTrustItem(client, TrustItem.TrustItemType.Company, company.uuid, modifyValues = {client -> modifiedClient = client})
        company.trustItemUuid = trustItem.uuid

        // Modify values
        name?.let {
            company.name = name
        }

        // Save it all
        var modifiedCompany = companyRepository.save(company)

        // Update trust
        trustService.updateTrust(trustItem)

        modifyValues(modifiedClient, modifiedCompany)
    }

    /**
     * Trust vote company.
     *
     * @param client        Client
     * @param company       Company
     * @param vote          Vote
     * @param modifyValues  Function called with modified client
     * @return              Trust vote
     */
    @ClientVerified
    open fun trustVoteCompany(client: Client, company: Company, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        return trustService.trustVoteItem(client, companyTrustItem(company), vote,
                modifyValues = {modifiedClient, trustItem -> modifyValues(modifiedClient)})
    }

    /**
     * Returns the trust item of the given company.
     *
     * @param company   Company
     * @return          Trust item
     */
    open fun companyTrustItem(company: Company): TrustItem {
        return trustItemRepository.findByUuid(company.trustItemUuid)!!
    }
}
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
import dk.etiktak.backend.model.trust.CompanyTrustVote
import dk.etiktak.backend.model.trust.TrustVoteType
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.trust.CompanyTrustVoteRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.security.ClientValid
import dk.etiktak.backend.service.security.ClientVerified
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
        private val trustVoteRepository: CompanyTrustVoteRepository) {

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
        val company = Company()
        company.uuid = CryptoUtil().uuid()
        company.name = name ?: ""

        var modifiedCompany = companyRepository.save(company)

        // Trust vote company
        var modifiedClient = client

        name?.let {
            trustVoteCompany(client, modifiedCompany, TrustVoteType.Trusted, modifyValues = { client, company -> modifiedClient = client; modifiedCompany = company })
        }

        modifyValues(modifiedClient)

        return modifiedCompany
    }

    /**
     * Edits a company.
     *
     * @param client        Client
     * @param company       Company
     * @param name          Name of company
     * @param modifyValues  Function called with modified company
     */
    @ClientVerified
    open fun editCompany(client: Client, company: Company, name: String?, modifyValues: (Company) -> Unit = {}) {

        // Modify values
        name?.let {
            company.name = name
        }

        // Save it all
        var modifiedCompany = companyRepository.save(company)

        // Recalculate trust
        recalculateCorrectnessTrust(modifiedCompany, modifyValues = {recalculatedCompany -> modifiedCompany = recalculatedCompany})

        modifyValues(modifiedCompany)
    }

    /**
     * Trust vote company correctness.
     *
     * @param client        Client
     * @param company       Company
     * @param vote          Vote
     * @param modifyValues  Function called with modified client and company
     * @return              Trust vote
     */
    @ClientValid
    open fun trustVoteCompany(client: Client, company: Company, vote: TrustVoteType, modifyValues: (Client, Company) -> Unit = {client, company -> Unit}): CompanyTrustVote {

        // Create trust vote
        val companyTrustVote = CompanyTrustVote()
        companyTrustVote.client = client
        companyTrustVote.company = company
        companyTrustVote.vote = vote

        // Glue it together
        company.correctnessTrustVotes.add(companyTrustVote)
        client.companyTrustVotes.add(companyTrustVote)

        // Save it all
        val modifiedCompanyTrustVote = trustVoteRepository.save(companyTrustVote)
        var modifiedCompany = companyRepository.save(company)
        val modifiedClient = clientRepository.save(client)

        // Recalculate trust
        recalculateCorrectnessTrust(modifiedCompany, modifyValues = {recalculatedCompany -> modifiedCompany = recalculatedCompany})

        modifyValues(modifiedClient, modifiedCompany)

        return modifiedCompanyTrustVote
    }

    /**
     * Recalculates company correctness trust.
     *
     * @param company        Company
     * @param modifyValues   Function called with modified company
     */
    open fun recalculateCorrectnessTrust(company: Company, modifyValues: (Company) -> Unit = {}) {

        // TODO! Calculate trust
    }
}
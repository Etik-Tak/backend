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

package dk.etiktak.backend.service.company

import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.company.CompanySearchEntry
import dk.etiktak.backend.model.contribution.Contribution
import dk.etiktak.backend.model.contribution.TextContribution
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.company.CompanyRepository
import dk.etiktak.backend.repository.company.CompanySearchEntryRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.service.trust.ContributionService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
@Transactional
open class CompanyService @Autowired constructor(
        private val companyRepository: CompanyRepository,
        private val contributionService: ContributionService,
        private val companySearchEntryRepository: CompanySearchEntryRepository,
        private val resourceLoader: ResourceLoader) {

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
     * Finds a company from the given name.
     *
     * @param name  Name
     * @return      Company with given name
     */
    open fun getCompanyByName(name: String): Company? {
        return companyRepository.findByNameIgnoreCase(name)
    }

    /**
     * Returns company search result matching the given search string.
     *
     * @param searchString  Search string
     * @return              Company search result
     */
    open fun getCompanySearchList(searchString: String): List<CompanySearchResult> {
        val companyEntries = companyRepository.findByNameIgnoreCaseContaining(searchString).map { it -> CompanySearchResult(it.name, company = it) }
        val companySearchEntries = companySearchEntryRepository.findByNameIgnoreCaseContaining(searchString).map { it -> CompanySearchResult(it.name, company = null) }

        return (companyEntries + companySearchEntries).distinctBy { it -> it.name }
    }

    /**
     * Creates a new company.
     *
     * @param inClient      Client
     * @param name          Name of company
     * @param modifyValues  Function called with modified client
     * @return              Created company
     */
    @ClientVerified
    open fun createCompany(inClient: Client, name: String, modifyValues: (Client) -> Unit = {}): Company {

        var client = inClient

        // Create company
        var company = Company()
        company.uuid = CryptoUtil().uuid()

        company = companyRepository.save(company)

        // Create name contribution
        editCompanyName(client, company, name, modifyValues = {modifiedClient, modifiedCompany -> client = modifiedClient; company = modifiedCompany})

        modifyValues(client)

        return company
    }

    /**
     * Edits a company name.
     *
     * @param inClient        Client
     * @param inCompany       Company
     * @param name            Name of company
     * @param modifyValues    Function called with modified client and company
     * @return                Name contribution
     */
    @ClientVerified
    open fun editCompanyName(inClient: Client, inCompany: Company, name: String, modifyValues: (Client, Company) -> Unit = {client, company -> Unit}): Contribution {

        var client = inClient
        var company = inCompany

        // Create contribution
        val contribution = contributionService.createTextContribution(Contribution.ContributionType.CompanyName, client, company.uuid, name, modifyValues = {modifiedClient -> client = modifiedClient})

        // Edit name
        company.name = name
        company = companyRepository.save(company)

        modifyValues(client, company)

        return contribution
    }

    /**
     * Returns the company name contribution which is currently active.
     *
     * @param company   Company
     * @return          Company name contribution
     */
    open fun companyNameContribution(company: Company): TextContribution? {
        return contributionService.currentTextContribution(Contribution.ContributionType.CompanyName, company.uuid)
    }

    /**
     * Returns whether the given client can edit the name of the company.
     *
     * @param client    Client
     * @param company   Company
     * @return          Yes,if the given client can edit the name of the company, or else false
     */
    open fun canEditCompanyName(client: Client, company: Company): Boolean {
        return client.verified && contributionService.hasSufficientTrustToEditContribution(client, companyNameContribution(company))
    }

    /**
     * Trust votes company name.
     *
     * @param client          Client
     * @param company         Company
     * @param vote            Vote
     * @param modifyValues    Function called with modified client
     * @return                Trust vote
     */
    open fun trustVoteCompanyName(client: Client, company: Company, vote: TrustVote.TrustVoteType, modifyValues: (Client) -> Unit = {}): TrustVote {
        val contribution = companyNameContribution(company)!!
        return contributionService.trustVoteItem(client, contribution, vote, modifyValues = {client, contribution -> modifyValues(client)})
    }

    /**
     * Populates search entry table with company names.
     */
    open fun populateSearchEntries() {
        val inputStream = resourceLoader.getResource("classpath:companies.txt").file.inputStream()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val companyNames = bufferedReader.readLines()

        for (name in companyNames) {
            if (companySearchEntryRepository.findByNameIgnoreCase(name) != null) {
                continue
            }

            // Create company search entry
            logger.info("Creating company search list entry: " + name)

            val companySearchList = CompanySearchEntry()
            companySearchList.uuid = CryptoUtil().uuid()
            companySearchList.name = name
            companySearchEntryRepository.save(companySearchList)
        }
    }
}

data class CompanySearchResult(
        val name: String,
        val company: Company?)

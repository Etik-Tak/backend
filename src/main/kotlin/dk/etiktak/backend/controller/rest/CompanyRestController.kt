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

/**
 * Rest controller responsible for handling company lifecycle.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.security.CurrentlyLoggedClient
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.company.CompanyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/company")
class CompanyRestController @Autowired constructor(
        private val companyService: CompanyService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getCompany(
            @CurrentlyLoggedClient loggedClient: Client?,
            @RequestParam(required = false) uuid: String?,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {

        // Check parameters
        if (StringUtils.isEmpty(uuid) && StringUtils.isEmpty(name)) {
            return illegalInvocationMap("Either 'companyUuid' or 'companyName' must be provided")
        }

        var company: Company? = null

        // Find company by UUID
        uuid?.let {
            company = companyService.getCompanyByUuid(uuid) ?: return notFoundMap("Company")
        }

        // Find company by name
        name?.let {
            company = companyService.getCompanyByName(name) ?: return notFoundMap("Company")
        }

        company?.let {
            return okMap().add(company!!, loggedClient, companyService = companyService)
        } ?: return notFoundMap("Company")
    }

    @RequestMapping(value = "/search/", method = arrayOf(RequestMethod.GET))
    fun findCompanies(
            @RequestParam searchString: String,
            @RequestParam(required = false) pageIndex: Int?,
            @RequestParam(required = false) pageSize: Int?): HashMap<String, Any> {

        val companySearchList = companyService.getCompanySearchList(searchString, pageIndex ?: 0, pageSize ?: 10)

        return okMap().add(companySearchList, companyService)
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createCompany(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam name: String): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")

        val company = companyService.createCompany(client, name)

        return okMap().add(company, client, companyService)
    }

    @RequestMapping(value = "/edit/", method = arrayOf(RequestMethod.POST))
    fun editCompany(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam companyUuid: String,
            @RequestParam(required = false) name: String?): HashMap<String, Any> {

        var client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        var company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")

        name?.let {
            companyService.editCompanyName(client, company, name, modifyValues = {modifiedClient, modifiedCompany -> client = modifiedClient; company = modifiedCompany})
        }

        return okMap().add(company, client, companyService)
    }

    @RequestMapping(value = "/trust/name/", method = arrayOf(RequestMethod.POST))
    fun trustVoteCompanyName(
            @CurrentlyLoggedClient loggedClient: Client,
            @RequestParam companyUuid: String,
            @RequestParam vote: TrustVote.TrustVoteType): HashMap<String, Any> {

        val client = clientService.getByUuid(loggedClient.uuid) ?: return notFoundMap("Client")
        val company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")

        companyService.trustVoteCompanyName(client, company, vote)

        return okMap()
    }
}

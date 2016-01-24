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

/**
 * Rest controller responsible for handling company lifecycle.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.add
import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.contribution.TrustVote
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.company.CompanyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/service/company")
class CompanyRestController @Autowired constructor(
        private val companyService: CompanyService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun getCompany(
            @RequestParam uuid: String): HashMap<String, Any> {
        val company = companyService.getCompanyByUuid(uuid) ?: return notFoundMap("Company")
        return companyOkMap(company)
    }

    @RequestMapping(value = "/create/", method = arrayOf(RequestMethod.POST))
    fun createCompany(
            @RequestParam clientUuid: String,
            @RequestParam name: String): HashMap<String, Any> {

        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")

        val company = companyService.createCompany(client, name)

        return companyOkMap(company)
    }

    @RequestMapping(value = "/edit/", method = arrayOf(RequestMethod.POST))
    fun editCompany(
            @RequestParam clientUuid: String,
            @RequestParam companyUuid: String,
            @RequestParam(required = false) name: String): HashMap<String, Any> {

        var client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        var company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")

        companyService.editCompany(client, company, name,
                modifyValues = {modifiedClient, modifiedCompany -> client = modifiedClient; company = modifiedCompany})

        return companyOkMap(company)
    }

    @RequestMapping(value = "/trust/", method = arrayOf(RequestMethod.POST))
    fun trustVoteCompany(
            @RequestParam clientUuid: String,
            @RequestParam companyUuid: String,
            @RequestParam vote: TrustVote.TrustVoteType): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap("Client")
        val company = companyService.getCompanyByUuid(companyUuid) ?: return notFoundMap("Company")

        companyService.trustVoteCompany(client, company, vote)

        return okMap()
    }

    fun companyOkMap(company: Company): HashMap<String, Any> {
        return okMap()
                .add("company", hashMapOf<String, Any>()
                        .add("uuid", company.uuid)
                        .add("name", company.name)
                        .add("trustScore", XXX)
    }
}

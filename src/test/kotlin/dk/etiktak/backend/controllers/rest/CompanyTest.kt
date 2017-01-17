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

package dk.etiktak.backend.controllers.rest

import dk.etiktak.backend.Application
import dk.etiktak.backend.controller.rest.WebserviceResult
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class CompanyTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "company/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
        client2DeviceId = createAndSaveClient()
    }

    /**
     * Test that we can create a company.
     */
    @Test
    fun createCompany() {
        mockMvc().perform(
                post(serviceEndpoint("create/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("name", "Coca Cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.company.uuid", notNullValue()))
                .andExpect(jsonPath("$.company.name", `is`("Coca Cola")))
    }

    /**
     * Test that we can retrieve a company by UUID.
     */
    @Test
    fun retrieveCompanyByUuid() {
        company1Uuid = createAndSaveCompany(client1DeviceId, "Pepsi Cola")

        mockMvc().perform(
                get(serviceEndpoint(""))
                        .param("uuid", company1Uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.company.uuid", `is`(company1Uuid)))
                .andExpect(jsonPath("$.company.name", `is`("Pepsi Cola")))
    }

    /**
     * Test that we can retrieve a company by name.
     */
    @Test
    fun retrieveCompanyByName() {
        company1Uuid = createAndSaveCompany(client1DeviceId, "Pepsi Cola")

        mockMvc().perform(
                get(serviceEndpoint(""))
                        .param("name", "pepsi cola"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.company.uuid", `is`(company1Uuid)))
                .andExpect(jsonPath("$.company.name", `is`("Pepsi Cola")))
    }

    /**
     * Test that we can search companies by name.
     */
    @Test
    fun searchCompanies() {
        company1Uuid = createAndSaveCompany(client1DeviceId, "BKI Kaffe")
        company2Uuid = createAndSaveCompany(client1DeviceId, "Merrild Kaffe")

        mockMvc().perform(
                get(serviceEndpoint("search/"))
                        .param("searchString", "kaffe"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.companies", Matchers.hasSize<Any>(3)))
                .andExpect(jsonPath("$.companies[0].name", `is`("BKI Kaffe")))
                .andExpect(jsonPath("$.companies[1].name", `is`("Merrild Kaffe")))
                .andExpect(jsonPath("$.companies[2].name", `is`("Peter Larsens Kaffe")))
    }
}

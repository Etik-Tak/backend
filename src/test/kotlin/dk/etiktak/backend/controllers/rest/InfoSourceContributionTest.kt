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

package dk.etiktak.backend.controllers.rest

import dk.etiktak.backend.Application
import dk.etiktak.backend.controller.rest.WebserviceResult
import dk.etiktak.backend.model.contribution.TrustVote
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class InfoSourceContributionTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "infosource/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1Uuid = createAndSaveClient()
        client2Uuid = createAndSaveClient()

        infoSource1Uuid = createAndSaveInfoSource(client1Uuid, arrayListOf("http://dr.dk"))
        infoSource2Uuid = createAndSaveInfoSource(client1Uuid, arrayListOf("http://politiken.dk"))
    }

    /**
     * Test that we can edit an info source name if we have sufficient trust level.
     */
    @Test
    fun editInfoSourceNameWhenSufficientTrusLevelt() {
        setClientTrustLevel(client1Uuid, 0.7)
        setInfoSourceNameTrustScore(infoSource1Uuid, 0.4)

        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("X-Auth-ClientUuid", client1Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("name", "New info source"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.infoSource.name", `is`("New info source")))
    }

    /**
     * Test that we cannot edit an info source name if we don't have sufficient trust level.
     */
    @Test
    fun cannotEditInfoSourceNameWhenInsufficientTrustLevel() {
        setClientTrustLevel(client1Uuid, 0.5)
        setInfoSourceNameTrustScore(infoSource1Uuid, 0.8)

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/edit/"))
                        .header("X-Auth-ClientUuid", client1Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("name", "New info source"))
    }

    /**
     * Test that a client can trust vote an info source name.
     */
    @Test
    fun trustVoteInfoSource() {
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("X-Auth-ClientUuid", client2Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }

    /**
     * Test that a client cannot trust vote more than once on same info source name.
     */
    @Test
    fun cannotTrustVoteMoreThanOnceOnSameInfoSourceName() {
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("X-Auth-ClientUuid", client2Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))

        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("X-Auth-ClientUuid", client2Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
    }

    /**
     * Test that a client cannot vote on info source names he edited himself.
     */
    @Test
    fun cannotTrustVoteOwnInfoSourceName() {
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("/trust/name/"))
                        .header("X-Auth-ClientUuid", client1Uuid)
                        .param("infoSourceUuid", infoSource1Uuid)
                        .param("vote", TrustVote.TrustVoteType.Trusted.name))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
    }
}

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
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.junit.Assert
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
open class InfoChannelServiceTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "infochannel/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1 = createAndSaveClient()
        client2 = createAndSaveClient()

        infoChannel1 = createAndSaveInfoChannel(client1)
        infoChannel2 = createAndSaveInfoChannel(client2)
    }

    /**
     * Test that we can create an info channel.
     */
    @Test
    fun createInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("create/"))
                        .param("clientUuid", client1.uuid)
                        .param("name", "Test Info Channel 1"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.infoChannel.uuid", Matchers.notNullValue()))
                .andExpect(jsonPath("$.infoChannel.name", `is`("Test Info Channel 1")))
    }

    /**
     * Test that we can follow an info channel.
     */
    @Test
    @Transactional
    fun followInfoChannel() {
        mockMvc().perform(
                post(serviceEndpoint("follow/"))
                        .param("clientUuid", client1.uuid)
                        .param("infoChannelUuid", infoChannel1.uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))

        Assert.assertEquals(
                1,
                infoChannelService!!.getInfoChannelByUuid(infoChannel1.uuid)!!.followers.size
        )

        mockMvc().perform(
                post(serviceEndpoint("follow/"))
                        .param("clientUuid", client1.uuid)
                        .param("infoChannelUuid", infoChannel2.uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))

        Assert.assertEquals(
                1,
                infoChannelService.getInfoChannelByUuid(infoChannel2.uuid)!!.followers.size
        )

        mockMvc().perform(
                post(serviceEndpoint("follow/"))
                        .param("clientUuid", client2.uuid)
                        .param("infoChannelUuid", infoChannel2.uuid))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))

        Assert.assertEquals(
                2,
                infoChannelService.getInfoChannelByUuid(infoChannel2.uuid)!!.followers.size
        )
    }
}

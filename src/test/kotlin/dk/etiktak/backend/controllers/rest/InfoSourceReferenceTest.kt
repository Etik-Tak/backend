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
import dk.etiktak.backend.model.product.Product
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class InfoSourceReferenceTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "infosourcereference/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1DeviceId = createAndSaveClient()
        client2DeviceId = createAndSaveClient()

        infoSource1Uuid = createAndSaveInfoSource(client1DeviceId, listOf("http://dr.dk", "http://www.dr.dk"))
        infoSource2Uuid = createAndSaveInfoSource(client2DeviceId, listOf("http://information.dk"))

        infoChannel1Uuid = createAndSaveInfoChannel(client1DeviceId)
        infoChannel2Uuid = createAndSaveInfoChannel(client2DeviceId)

        product1Uuid = createAndSaveProduct(client1DeviceId, "12345678", Product.BarcodeType.EAN_13)
        product2Uuid = createAndSaveProduct(client2DeviceId, "87654321", Product.BarcodeType.EAN_13)

        productCategory1Uuid = createAndSaveProductCategory(client1DeviceId, product1Uuid)
        productCategory2Uuid = createAndSaveProductCategory(client2DeviceId, product2Uuid)

        productLabel1Uuid = createAndSaveProductLabel(client1DeviceId, product1Uuid)
        productLabel2Uuid = createAndSaveProductLabel(client2DeviceId, product2Uuid)

        productRecommendation1Uuid = createAndSaveProductRecommendation(client1DeviceId, infoChannel1Uuid, product1Uuid)
        productRecommendation2Uuid = createAndSaveProductRecommendation(client2DeviceId, infoChannel2Uuid, product2Uuid)
    }

    /**
     * Test that we can create an info source reference.
     */
    @Test
    fun createInfoSourceReference() {
        mockMvc().perform(
                post(serviceEndpoint("create/"))
                        .header("X-Auth-DeviceId", client1DeviceId)
                        .param("recommendationUuid", productRecommendation1Uuid)
                        .param("url", "http://www.dr.dk/nyheder/viden/miljoe/foedevarestyrelsen-spis-ikke-meget-moerk-chokolade")
                        .param("title", "Fødevarestyrelsen: Spis ikke for meget mørk chokolade"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", `is`(WebserviceResult.OK.value)))
                .andExpect(jsonPath("$.infoSourceReference.uuid", notNullValue()))
                .andExpect(jsonPath("$.infoSourceReference.url", `is`("http://www.dr.dk/nyheder/viden/miljoe/foedevarestyrelsen-spis-ikke-meget-moerk-chokolade")))
                .andExpect(jsonPath("$.infoSourceReference.title", `is`("Fødevarestyrelsen: Spis ikke for meget mørk chokolade")))
    }
}

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
import org.springframework.http.MediaType
import org.springframework.web.util.NestedServletException

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(Application::class))
@WebAppConfiguration
class InfoSourceReferenceServiceTest : BaseRestTest() {

    fun serviceEndpoint(postfix: String): String {
        return super.serviceEndpoint() + "infosourcereference/" + postfix
    }

    @Before
    override fun setup() {
        super.setup()

        client1 = createAndSaveClient()
        client2 = createAndSaveClient()

        infoSource1 = createAndSaveInfoSource(client1, listOf("http://dr.dk", "http://www.dr.dk"))
        infoSource2 = createAndSaveInfoSource(client2, listOf("http://information.dk"))

        infoChannel1 = createAndSaveInfoChannel(client1)
        infoChannel2 = createAndSaveInfoChannel(client2)

        product1 = createAndSaveProduct(client1, "12345678", Product.BarcodeType.EAN13)
        product2 = createAndSaveProduct(client2, "87654321", Product.BarcodeType.EAN13)
    }

    /**
     * Test that we can create an info source reference.
     */
    @Test
    fun createInfoSourceReference() {
        mockMvc().perform(
                post(serviceEndpoint("create/"))
                        .param("clientUuid", client1.uuid)
                        .param("infoChannelUuid", infoChannel1.uuid)
                        .param("infoSourceUuid", infoSource1.uuid)
                        .param("url", "http://www.dr.dk/nyheder/viden/miljoe/foedevarestyrelsen-spis-ikke-meget-moerk-chokolade")
                        .param("title", "Fødevarestyrelsen: Spis ikke for meget mørk chokolade")
                        .param("summary", "Visse mørke chokolader indeholder bekymrende meget cadmium, viser test i Videnskabsmagasinet på DR3."))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.infoSourceReference.uuid", notNullValue()))
                .andExpect(jsonPath("$.infoSourceReference.url", `is`("http://www.dr.dk/nyheder/viden/miljoe/foedevarestyrelsen-spis-ikke-meget-moerk-chokolade")))
                .andExpect(jsonPath("$.infoSourceReference.title", `is`("Fødevarestyrelsen: Spis ikke for meget mørk chokolade")))
                .andExpect(jsonPath("$.infoSourceReference.summary", `is`("Visse mørke chokolader indeholder bekymrende meget cadmium, viser test i Videnskabsmagasinet på DR3.")))
    }

    /**
     * Test that we cannot create an info source reference with an url that is not in the domain of the info source.
     */
    @Test
    fun createInfoSourceReferanceWithIllegalUrlDomain() {
        exception.expect(NestedServletException::class.java)
        mockMvc().perform(
                post(serviceEndpoint("create/"))
                        .param("clientUuid", client1.uuid)
                        .param("infoChannelUuid", infoChannel1.uuid)
                        .param("infoSourceUuid", infoSource1.uuid)
                        .param("url", "http://politiken.dk/forbrugogliv/forbrug/tjekmad/ECE2981742/eksperter-advarer-glutenfri-foedevarer-er-slet-ikke-sunde/")
                        .param("title", "Eksperter advarer: Glutenfri fødevarer er slet ikke sunde")
                        .param("summary", "Glutenfri fødevarer opfattes som sunde, men er ofte det modsatte, lyder det fra eksperter."))
                .andExpect(status().`is`(400))
    }

    /**
     * Test that we can assign products to an existing info source reference.
     */
    @Test
    fun assignProductsToExistingInfoSourceReference() {
        val infoSourceReference = infoSourceReferenceService!!.createInfoSourceReference(
                client1,
                infoChannel1,
                infoSource1,
                "http://www.dr.dk/nyheder/viden/miljoe/foedevarestyrelsen-spis-ikke-meget-moerk-chokolade",
                "Fødevarestyrelsen: Spis ikke for meget mørk chokolade",
                "Visse mørke chokolader indeholder bekymrende meget cadmium, viser test i Videnskabsmagasinet på DR3.")

        mockMvc().perform(
                post(serviceEndpoint("assign/products/"))
                        .param("clientUuid", client1.uuid)
                        .param("infoSourceReferenceUuid", infoSourceReference.uuid)
                        .param("productUuids", "${product1.uuid}, ${product2.uuid}"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.message", `is`(WebserviceResult.OK.name)))
                .andExpect(jsonPath("$.infoSourceReference.uuid", notNullValue()))
    }
}

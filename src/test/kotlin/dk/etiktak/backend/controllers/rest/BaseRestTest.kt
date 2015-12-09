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

import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.Charset

import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

open class BaseRestTest {

    fun serviceEndpoint(): String {
        return "/service/"
    }

    @Autowired
    private val webApplicationContext: WebApplicationContext? = null

    @Autowired
    internal fun setConverters(converters: Array<HttpMessageConverter<Any>>) {
        this.mappingJackson2HttpMessageConverter = converters.asList().filter(
                { hmc -> hmc is MappingJackson2HttpMessageConverter }).first()

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter)
    }

    protected var jsonContentType = MediaType(
            MediaType.APPLICATION_JSON.type,
            MediaType.APPLICATION_JSON.subtype,
            Charset.forName("utf8"))

    protected var mockMvcVar: MockMvc? = null

    protected var mappingJackson2HttpMessageConverter: HttpMessageConverter<Any>? = null

    @Throws(Exception::class)
    open fun setup() {
        mockMvcVar = webAppContextSetup(webApplicationContext).build()
    }

    fun mockMvc(): MockMvc {
        return mockMvcVar!!
    }
}
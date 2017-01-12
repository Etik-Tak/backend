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
 * The base class of the rest controllers. Includes utility methods to
 * easily create model maps.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.addMessage
import dk.etiktak.backend.controller.rest.json.addResult
import java.util.*

enum class WebserviceResult(val value: Int, val message: String) {
    OK(0, "OK"),
    NotFound(1, "not found"),
    IllegalInvocation(2, "Illegal invocation of endpoint")
}

open class BaseRestController {
    fun okMap() : HashMap<String, Any> {
        return hashMapOf<String, Any>()
                .addResult(WebserviceResult.OK.value)
                .addMessage(WebserviceResult.OK.message)
    }

    /**
     * Return a hashmap where not found message has been set.
     *
     * @param entity   Entity name
     * @return         Hash map
     */
    fun notFoundMap(entity: String) : HashMap<String, Any> {
        return hashMapOf<String, Any>()
                .addResult(WebserviceResult.NotFound.value)
                .addMessage(entity + " " + WebserviceResult.NotFound.message)
    }

    /**
     * Return a hashmap where illegal invocation message has been set.
     *
     * @param message Optional message
     * @return        Hash map
     */
    fun illegalInvocationMap(message: String = WebserviceResult.IllegalInvocation.message) : HashMap<String, Any> {
        return hashMapOf<String, Any>()
                .addResult(WebserviceResult.IllegalInvocation.value)
                .addMessage(message)
    }
}

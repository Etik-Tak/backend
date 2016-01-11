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
 * Utility (extension) methods for converting model objects to JSON model maps.
 */

package dk.etiktak.backend.controller.rest.json

import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("Jsonifier")

/**
 * Adds a result to the hash map.
 *
 * @param result   Result to add
 * @return         Self with result mapped
 */
fun HashMap<String, Any>.addResult(result: Int) : HashMap<String, Any> {
    this["result"] = result
    return this
}

/**
 * Adds a message to the hash map.
 *
 * @param message  Message to add
 * @return         Self with message mapped
 */
fun HashMap<String, Any>.addMessage(message: String) : HashMap<String, Any> {
    this["message"] = message
    return this
}

/**
 * Adds an entity to the map. If entity is null the function is idempotence.
 *
 * @param jsonKey   Key to map entity to
 * @param entity    Entity to map
 * @return          Map with entity mapped
 */
fun <T> HashMap<String, T>.add(jsonKey: String, entity: T?) : HashMap<String, T> {
    entity?.let {
        this[jsonKey] = entity
    }
    return this
}

/**
 * Transforms and adds a list of entities to the list. If entities is null the function is idempotence.
 *
 * @param jsonKey   Key to map array to
 * @param entity    Entity to map list to
 * @return          Hash map with entity list mapped
 */
fun <T> HashMap<String, Any>.add(jsonKey: String, entities: Collection<T>?, transformer: (T) -> Any) : HashMap<String, Any> {
    entities?.let {
        val list: MutableList<Any> = arrayListOf()
        for (entity in entities) {
            list.add(transformer(entity))
        }
        this[jsonKey] = list
    }
    return this
}

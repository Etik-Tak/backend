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
 * Extension methods for converting model objects to JSON model maps.
 */

package dk.etiktak.backend.controller.rest.json

import dk.etiktak.backend.model.company.Company
import dk.etiktak.backend.model.company.Store
import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.recommendation.Recommendation
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.model.user.SmsVerification
import dk.etiktak.backend.service.company.CompanyService
import dk.etiktak.backend.service.company.StoreService
import dk.etiktak.backend.service.product.ProductService
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("Jsonifier")

/* Utility methods for adding standard representations of model objects. */

fun HashMap<String, Any>.add(product: Product, client: Client? = null, productService: ProductService): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("product", hashMapOf<String, Any>()
                    .add("uuid", product.uuid)
                    .add("name", product.name)
                    .add("categories", product.productCategories, { category -> hashMapOf<String, Any>()
                            .add("uuid", category.uuid)
                            .add("name", category.name) })
                    .add("labels", product.productLabels, { label -> hashMapOf<String, Any>()
                            .add("uuid", label.uuid)
                            .add("name", label.name) })
                    .add("editableItems", {client != null}, {hashMapOf<String, Any>()
                            .add("name", hashMapOf<String, Any>()
                                    .add("editable", {productService.canEditProductName(client!!, product)})
                                    .add("trustScore", {productService.productNameContribution(product)?.trustScore}))})))
}

fun HashMap<String, Any>.add(company: Company, client: Client? = null, companyService: CompanyService): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("company", hashMapOf<String, Any>()
                    .add("uuid", company.uuid)
                    .add("name", company.name)
                    .add("editableItems", {client != null}, {hashMapOf<String, Any>()
                            .add("name", hashMapOf<String, Any>()
                                    .add("editable", {companyService.canEditCompanyName(client!!, company)})
                                    .add("trustScore", {companyService.companyNameContribution(company)?.trustScore}))})))
}

fun HashMap<String, Any>.add(store: Store, client: Client? = null, storeService: StoreService): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("store", hashMapOf<String, Any>()
                    .add("uuid", store.uuid)
                    .add("name", storeService.storeName(store))
                    .add("editableItems", {client != null}, {hashMapOf<String, Any>()
                            .add("name", hashMapOf<String, Any>()
                                    .add("editable", {storeService.canEditStoreName(client!!, store)})
                                    .add("trustScore", {storeService.storeNameContribution(store)?.trustScore}))})))
}

fun HashMap<String, Any>.add(productLabel: ProductLabel): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("productLabel", hashMapOf<String, Any>()
                    .add("uuid", productLabel.uuid)
                    .add("name", productLabel.name)))
}

fun HashMap<String, Any>.add(productCategory: ProductCategory): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("productCategory", hashMapOf<String, Any>()
                    .add("uuid", productCategory.uuid)
                    .add("name", productCategory.name)))
}

fun HashMap<String, Any>.add(recommendation: Recommendation): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("recommendation", hashMapOf<String, Any>()
                    .add("uuid", recommendation.uuid)
                    .add("summary", recommendation.summary)
                    .add("score", recommendation.score.name)))
}

fun HashMap<String, Any>.add(recommendations: List<Recommendation>): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("recommendations", recommendations, { recommendation -> hashMapOf<String, Any>()
                    .add("uuid", recommendation.uuid)
                    .add("summary", recommendation.summary)
                    .add("score", recommendation.score.name) }))
}

fun HashMap<String, Any>.add(infoSource: InfoSource): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("infoSource", hashMapOf<String, Any>()
                    .add("uuid", infoSource.uuid)
                    .add("friendlyName", infoSource.friendlyName)
                    .add("urlPrefixes", infoSource.urlPrefixes, { urlPrefix -> urlPrefix.urlPrefix })))
}

fun HashMap<String, Any>.add(infoSourceReference: InfoSourceReference): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("infoSourceReference", hashMapOf<String, Any>()
                    .add("uuid", infoSourceReference.uuid)
                    .add("url", infoSourceReference.url)
                    .add("title", infoSourceReference.title)
                    .add("summary", infoSourceReference.summary)
                    .add("categories", infoSourceReference.productCategories, { category -> hashMapOf<String, Any>()
                            .add("uuid", category.uuid)
                            .add("name", category.name) })
                    .add("labels", infoSourceReference.productLabels, { label -> hashMapOf<String, Any>()
                            .add("uuid", label.uuid)
                            .add("name", label.name) })
                    .add("companies", infoSourceReference.companies, { company -> hashMapOf<String, Any>()
                            .add("uuid", company.uuid)
                            .add("name", company.name) })))
}

fun HashMap<String, Any>.add(infoChannel: InfoChannel): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("infoChannel", hashMapOf<String, Any>()
                    .add("uuid", infoChannel.uuid)
                    .add("name", infoChannel.name)))
}

fun HashMap<String, Any>.add(client: Client): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("client", hashMapOf<String, Any>()
                    .add("uuid", client.uuid)))
}

fun HashMap<String, Any>.add(smsVerification: SmsVerification): HashMap<String, Any> {
    return add(hashMapOf<String, Any>()
            .add("smsVerification", hashMapOf<String, Any>()
                    .add("challenge", smsVerification.clientChallenge)
                    .add("status", smsVerification.status.name)))
}



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
 * Adds an entity to the map given by the evaluation function. If entity is null the function is idempotence.
 *
 * @param jsonKey     Key to map entity to
 * @param condition   Condition
 * @param entity      Entity to map
 * @return            Map with entity mapped if condition is true
 */
fun <T> HashMap<String, T>.add(jsonKey: String, valueFunction: () -> T?) : HashMap<String, T> {
    val entity = valueFunction()
    entity?.let {
        this[jsonKey] = entity
    }
    return this
}

/**
 * Adds all entries of given map to the map.
 *
 * @param entity    Entity to map
 * @return          Map with entity mapped
 */
fun <T> HashMap<String, T>.add(entries: HashMap<String, T>) : HashMap<String, T> {
    for (jsonKey in entries.keys) {
        val entity = entries[jsonKey]
        entity?.let {
            this[jsonKey] = entity
        }
    }
    return this
}

/**
 * Adds an entity to the map if the given condition evaluated to true. If entity is null the function is idempotence.
 *
 * @param jsonKey     Key to map entity to
 * @param condition   Condition
 * @param entity      Entity to map
 * @return            Map with entity mapped if condition is true
 */
fun <T> HashMap<String, T>.add(jsonKey: String, condition: () -> Boolean, entityFunction: () -> T?) : HashMap<String, T> {
    if (condition()) {
        val entity = entityFunction()
        entity?.let {
            this[jsonKey] = entity
        }
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

/**
 * Adds the given item to the list if the given condition evaluated to true. If entity is null the function is idempotence.
 *
 * @param item        Item to add
 * @param condition   Condition
 * @return            List with item added if condition is true
 */
fun <T> ArrayList<T>.add(item: T, condition: () -> Boolean) : ArrayList<T> {
    if (condition()) {
        add(item)
    }
    return this
}

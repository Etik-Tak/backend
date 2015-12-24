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

import dk.etiktak.backend.util.asList
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils
import java.lang.reflect.Field
import java.util.*

private val logger = LoggerFactory.getLogger("Jsonifier")

private val annotationCache: MutableMap<String, List<Field>> = HashMap()



enum class JsonifyRule() {
    COMPLETE,
    NORMAL,
    THIN
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Jsonifier(
        val jsonKey: String = "",
        val rules: Array<JsonifyRule> = arrayOf(),
        val simpleListFieldName: String = "",
        val extractFieldNames: Array<String> = arrayOf()
)



/**
 * Adds a message to the hash map.
 * @param message  Message to add
 * @return         Self with message mapped
 */
fun HashMap<String, Any>.addMessage(message: String) : HashMap<String, Any> {
    this["message"] = message
    return this
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any?): HashMap<String, Any> {
    return addEntity(entity, rule = JsonifyRule.NORMAL, deep = true)
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @param rule    Rule to match
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any?, rule: JsonifyRule): HashMap<String, Any> {
    return addEntity(entity, rule, deep = true)
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @param rule    Rule to match
 * @param deep    If true, follow lists recursively
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any?, rule: JsonifyRule, deep: Boolean): HashMap<String, Any> {
    return addEntity(entity, fields = annotatedFields(entity), rule = rule, deep = deep)
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @param fields  Fields to map
 * @param rule    Rule to match
 * @param deep    If true, follow lists recursively
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any?, fields: List<Field>, rule: JsonifyRule, deep: Boolean): HashMap<String, Any> {
    if (entity == null) {
        return this
    }

    val entityMap = HashMap<String, Any>()
    for (field in fields) {
        entityMap.addEntity(entity, field, rule, deep)
    }

    val jsonKey = jsonKeyFromEntity(entity)
    this[jsonKey] = entityMap

    return this
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @param field   Field to map
 * @param rule    Rule to match
 * @param deep    If true, follow lists recursively
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any, field: Field, rule: JsonifyRule, deep: Boolean): HashMap<String, Any> {
    val annotation = field.getAnnotation(Jsonifier::class.java)

    if (!annotation.rules.contains(rule)) {
        return this
    }

    val jsonKey = if (!StringUtils.isEmpty(annotation.jsonKey)) annotation.jsonKey else field.name

    if (field.type == List::class.java || field.type == Set::class.java) {
        if (deep) {
            field.isAccessible = true
            val entries = ArrayList<Any>()
            if (field.type == List::class.java) {
                entries.addAll(field.get(entity) as List<Any>)
            }
            if (field.type == Set::class.java) {
                entries.addAll(field.get(entity) as Set<Any>)
            }
            if (!StringUtils.isEmpty(annotation.simpleListFieldName)) {
                this[jsonKey] = extractSimpleListFromFieldName(entries, annotation.simpleListFieldName)
            /*} else if (!StringUtils.isEmpty(annotation.extractFieldNames)) {
                    this[jsonKey] = extractListFromFieldNames(entries, annotation.extractFieldNames)*/
            } else {
                this[jsonKey] = entityList(entries, rule, deep)
            }
        }
    } else {
        this.addEntity(entity, field, jsonKey, rule, deep)
    }
    return this
}

/**
 * Converts the given entity to json.
 *
 * @param entity  Entity to map
 * @param field   Field to map
 * @param jsonKey Key to map to
 * @param rule      Rule to match
 * @param deep      If true, follow lists recursively
 * @return        Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any, field: Field, jsonKey: String, rule: JsonifyRule, deep: Boolean): HashMap<String, Any> {
    field.isAccessible = true
    val value = field.get(entity)
    value?.let {
        if (value.javaClass.getAnnotation(Jsonifier::class.java) != null) {
            addEntity(value, rule, deep)
        } else {
            this[jsonKey] = value
        }
    }
    return this
}

/**
 * Converts and returns a list of mapped entities.
 *
 * @param entities  Entity list to map
 * @param rule      Rule to match
 * @param deep      If true, follow lists recursively
 * @return          List of mapped entities
 */
fun entityList(entities: List<Any>, rule: JsonifyRule, deep: Boolean): List<Any> {
    val result: MutableList<Any> = ArrayList()
    for (entity in entities) {
        result.add(HashMap<String, Any>().addEntity(entity, rule, deep))
    }
    return result
}

/**
 * Extracts a list of values from the given entities for the field with the given key.
 *
 * @param entities   Entity list to map
 * @param fieldName  Field name to extract
 * @return           List of field values
 */
fun extractSimpleListFromFieldName(entities: List<Any>, fieldName: String): List<Any> {
    val result: MutableList<Any> = ArrayList()
    for (entity in entities) {
        logger.info("FIeld: " + entity.javaClass.canonicalName)
        val field = entity.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        result.add(field.get(entity))
    }
    return result
}

/**
 * Returns the @Jsonifier annotated fields of the given entity using the annotation cache.
 *
 * @param entity  Entity to scan
 * @return        A list of fields annotated with @Jsonifier
 */
private fun annotatedFields(entity: Any?): List<Field> {
    if (entity == null) {
        return ArrayList()
    }

    synchronized(annotationCache, {

        // Check if cached
        val fields = annotationCache[entity.javaClass.name]
        if (fields != null) {
            return fields
        }

        logger.info("Scanning for JSON annotations on entity: " + entity.javaClass.canonicalName)

        // Scan entity for @Jsonifier fields
        val annotatedFields: MutableList<Field> = ArrayList()

        for (field in entity.javaClass.declaredFields) {
            if (field.isAnnotationPresent(Jsonifier::class.java)) {
                logger.info("Found field: " + field.name)
                annotatedFields.add(field)
            }
        }

        // Cache fields
        annotationCache[entity.javaClass.name] = annotatedFields

        return annotatedFields
    })
}

/**
 * Returns the json key associated with the given entity.
 *
 * @param entity  Entity
 * @return        Associated json key, if any, or else name of class
 */
fun jsonKeyFromEntity(entity: Any): String {
    val annotation = entity.javaClass.getAnnotation(Jsonifier::class.java)
    return if (annotation != null && !StringUtils.isEmpty(annotation.jsonKey)) annotation.jsonKey else entity.javaClass.canonicalName
}

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
import org.springframework.util.StringUtils
import java.lang.reflect.Field
import java.util.*
import javax.persistence.Entity

private val logger = LoggerFactory.getLogger("Jsonifier")

private val annotationCache: MutableMap<String, List<Field>> = HashMap()



enum class JsonFilter() {
    RETRIEVE,
    CREATE
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Jsonifier(
        val key: String = "",
        val filter: Array<JsonFilter> = arrayOf(),
        val extractFieldFromReference: String = ""
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
 * Adds a map to the the hash map.
 *
 * @param key   Key
 * @param map   Optional map
 * @return      Self with map added
 */
fun HashMap<String, Any>.addMap(key: String, map: Map<String, Any>?) : HashMap<String, Any> {
    map?.let {
        this[key] = map
    }
    return this
}

/**
 * Converts the given entity to json.
 *
 * @param entity   Entity to map
 * @param filter   Filter to match
 * @return         Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(entity: Any?, filter: JsonFilter): HashMap<String, Any> {
    return addEntity(parentEntities = ArrayList(), entity = entity, filter = filter)
}

/**
 * Converts the given entity to json.
 *
 * @param parentEntities  Parent entities (used to avoid cycles)
 * @param entity   Entity to map
 * @param filter   Filter to match
 * @return         Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(parentEntities: MutableList<Any>, entity: Any?, filter: JsonFilter): HashMap<String, Any> {
    return addEntity(parentEntities = parentEntities, entity = entity, fields = annotatedFields(entity), filter = filter)
}

/**
 * Converts the given entity to json.
 *
 * @param parentEntities  Parent entities (used to avoid cycles)
 * @param entity          Entity to map
 * @param fields          Fields to map
 * @param filter          Filter to match
 * @return                Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(parentEntities: MutableList<Any>, entity: Any?, fields: List<Field>, filter: JsonFilter): HashMap<String, Any> {
    if (entity == null) {
        return this
    }

    // Avoid cycles
    if (parentEntities.contains(entity)) {
        return this
    }
    parentEntities.add(entity)

    val entityMap = HashMap<String, Any>()
    for (field in fields) {
        entityMap.addEntity(parentEntities, entity, field, filter)
    }

    val jsonKey = jsonKeyFromEntity(entity)
    this[jsonKey] = entityMap

    return this
}

/**
 * Converts the given entity to json.
 *
 * @param parentEntities  Parent entities (used to avoid cycles)
 * @param entity          Entity to map
 * @param field           Field to map
 * @param filter          Filter to match
 * @param deep            If true, follow lists recursively
 * @return                Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(parentEntities: MutableList<Any>, entity: Any, field: Field, filter: JsonFilter): HashMap<String, Any> {
    val annotation = field.getAnnotation(Jsonifier::class.java)

    if (!annotation.filter.contains(filter)) {
        return this
    }

    val jsonKey = if (!StringUtils.isEmpty(annotation.key)) annotation.key else field.name

    if (field.type == List::class.java || field.type == Set::class.java) {
        field.isAccessible = true
        val entries = ArrayList<Any>()
        if (field.type == List::class.java) {
            entries.addAll(field.get(entity) as List<Any>)
        }
        if (field.type == Set::class.java) {
            entries.addAll(field.get(entity) as Set<Any>)
        }
        if (!StringUtils.isEmpty(annotation.extractFieldFromReference)) {
            this[jsonKey] = extractSimpleListFromFieldName(entries, annotation.extractFieldFromReference)
        } else {
            this[jsonKey] = entityList(parentEntities, entries, filter)
        }
    } else {
        this.addEntity(parentEntities, entity, field, jsonKey, filter)
    }
    return this
}

/**
 * Converts the given entity to json.
 *
 * @param entity          Entity to map
 * @param parentEntities  Parent entities (used to avoid cycles)
 * @param field           Field to map
 * @param jsonKey         Key to map to
 * @param filter          Filter to match
 * @return                Self with entity mapped
 */
fun HashMap<String, Any>.addEntity(parentEntities: MutableList<Any>, entity: Any, field: Field, jsonKey: String, filter: JsonFilter): HashMap<String, Any> {
    field.isAccessible = true
    val value = field.get(entity)
    value?.let {
        if (value.javaClass.getAnnotation(Jsonifier::class.java) != null) {
            addEntity(parentEntities, value, filter)
        } else {
            this[jsonKey] = value
        }
    }
    return this
}

/**
 * Converts and returns a list of mapped entities.
 *
 * @param parentEntities  Parent entities (used to avoid cycles)
 * @param entities        Entity list to map
 * @param filter          Filter to match
 * @param deep            If true, follow lists recursively
 * @return                List of mapped entities
 */
fun entityList(parentEntities: MutableList<Any>, entities: List<Any>, filter: JsonFilter): List<Any> {
    val result: MutableList<Any> = ArrayList()
    for (entity in entities) {
        result.add(HashMap<String, Any>().addEntity(parentEntities, entity, filter))
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
        fields?.let {
            return fields
        }

        // Scan entity for @Jsonifier fields
        val annotatedFields: MutableList<Field> = ArrayList()

        var currentEntity: Class<in Any> = entity.javaClass
        while (isEntityClass(currentEntity) || currentEntity == entity.javaClass) {
            logger.info("Scanning for JSON annotations on entity: " + currentEntity.canonicalName)

            for (field in currentEntity.declaredFields) {
                if (field.isAnnotationPresent(Jsonifier::class.java)) {
                    logger.info("Found field: " + field.name)
                    annotatedFields.add(field)
                }
            }
            currentEntity = currentEntity.superclass
        }

        // Cache fields
        annotationCache[entity.javaClass.name] = annotatedFields

        return annotatedFields
    })
}

/**
 * Returns whether or not the given object if an entity.
 *
 * @param obj  Object
 * @return     True, if given object is an instance of a entity class, or else false
 *
 */
fun isEntityClass(obj: Class<in Any>): Boolean {
    return obj.isAnnotationPresent(Entity::class.java)
}

/**
 * Returns the json key associated with the given entity.
 *
 * @param entity  Entity
 * @return        Associated json key, if any, or else name of class
 */
fun jsonKeyFromEntity(entity: Any): String {
    val annotation = entity.javaClass.getAnnotation(Jsonifier::class.java)
    return if (annotation != null && !StringUtils.isEmpty(annotation.key)) annotation.key else entity.javaClass.canonicalName
}

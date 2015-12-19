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

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.infosource.InfoSourceUrlPrefix
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.model.user.SmsVerification
import java.util.*

fun HashMap<String, Any>.addMessage(message: String) : HashMap<String, Any> {
    this["message"] = message
    return this
}

fun HashMap<String, Any>.add(client: Client?) : HashMap<String, Any> {
    client?.let {
        this["client"] = hashMapOf(
                "uuid" to client.uuid,
                "verified" to client.verified)
    }
    return this
}

fun HashMap<String, Any>.add(location: Location?) : HashMap<String, Any> {
    location?.let {
        this["location"] = hashMapOf<String, Any>(
                "latitude" to location.latitude,
                "longitude" to location.longitude)
    }
    return this
}

fun HashMap<String, Any>.add(product: Product?) : HashMap<String, Any> {
    product?.let {
        this["product"] = hashMapOf<String, Any>(
                "uuid" to product.uuid,
                "name" to product.name,
                "barcode" to product.barcode,
                "barcodeType" to product.barcodeType.name)
    }
    return this
}

fun HashMap<String, Any>.add(productScan: ProductScan?) : HashMap<String, Any> {
    productScan?.let {
        val map = hashMapOf<String, Any>(
                "uuid" to productScan.uuid
        )
        map.add(productScan.product)
        productScan.location?.let {
            map.add(productScan.location)
        }
        this["productScan"] = map
    }
    return this
}

fun HashMap<String, Any>.add(smsVerification: SmsVerification?) : HashMap<String, Any> {
    smsVerification?.let {
        val map = hashMapOf<String, Any>()
        smsVerification.clientChallenge?.let {
            map["challenge"] = smsVerification.clientChallenge as Any
        }
        this["smsVerification"] = map
    }
    return this
}

fun HashMap<String, Any>.add(infoChannel: InfoChannel?) : HashMap<String, Any> {
    infoChannel?.let {
        val map = hashMapOf<String, Any>()
        map["uuid"] = infoChannel.uuid
        map["name"] = infoChannel.name
        this["infoChannel"] = map
    }
    return this
}

fun HashMap<String, Any>.add(infoSource: InfoSource?) : HashMap<String, Any> {
    infoSource?.let {
        val map = hashMapOf<String, Any>()
        map["uuid"] = infoSource.uuid
        map["friendlyName"] = infoSource.friendlyName
        map.add(infoSource.urlPrefixes)
        this["infoSource"] = map
    }
    return this
}

fun HashMap<String, Any>.add(infoSourceReference: InfoSourceReference?) : HashMap<String, Any> {
    infoSourceReference?.let {
        val map = hashMapOf<String, Any>()
        map["uuid"] = infoSourceReference.uuid
        map["url"] = infoSourceReference.url
        map["title"] = infoSourceReference.title
        map["summaryMarkdown"] = infoSourceReference.summaryMarkdown
        this["infoSourceReference"] = map
    }
    return this
}

fun HashMap<String, Any>.add(urlPrefixes: List<InfoSourceUrlPrefix>?) : HashMap<String, Any> {
    urlPrefixes?.let {
        val list: MutableList<String> = ArrayList()
        System.out.println("HEY")
        for (urlPrefix in urlPrefixes) {
            System.out.println("-----> " + urlPrefix)
            list.add(urlPrefix.urlPrefix)
        }
        this["urlPrefixes"] = list
    }
    return this
}

fun HashMap<String, Any>.add(infoSourceUrlPrefix: InfoSourceUrlPrefix?) : HashMap<String, Any> {
    infoSourceUrlPrefix?.let {
        val map = hashMapOf<String, Any>()
        map["uuid"] = infoSourceUrlPrefix.uuid
        map["urlPrefix"] = infoSourceUrlPrefix.urlPrefix
        this["infoSourceUrlPrefix"] = map
    }
    return this
}

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

package dk.etiktak.backend.controller.rest.json

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

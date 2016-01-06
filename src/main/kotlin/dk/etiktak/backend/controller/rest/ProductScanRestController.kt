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
 * Rest controller responsible for handling product scans.
 */

package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controller.rest.json.JsonFilter
import dk.etiktak.backend.controller.rest.json.addEntity
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/service/product/scan")
class ProductScanRestController @Autowired constructor(
        private val productService: ProductService,
        private val clientService: ClientService) : BaseRestController() {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.POST))
    fun scanProduct(
            @RequestParam barcode: String,
            @RequestParam clientUuid: String,
            @RequestParam(required = false) latitude: String? = null,
            @RequestParam(required = false) longitude: String? = null): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()

        var location: Location? = null
        if (latitude != null && longitude != null) {
            location = Location(latitude.toDouble(), longitude.toDouble())
        }

        val productScanResult = productService.scanProduct(barcode, client, location)

        return okMap().addEntity(productScanResult, JsonFilter.RETRIEVE)
    }

    @RequestMapping(value = "/assign/location/", method = arrayOf(RequestMethod.POST))
    fun provideProductScanLocation(
            @RequestParam clientUuid: String,
            @RequestParam productScanUuid: String,
            @RequestParam latitude: String,
            @RequestParam longitude: String): HashMap<String, Any> {
        val client = clientService.getByUuid(clientUuid) ?: return notFoundMap()
        var productScan = productService.getProductScanByUuid(productScanUuid) ?: return notFoundMap()

        val location = Location(latitude.toDouble(), longitude.toDouble())

        productService.assignLocationToProductScan(client, productScan, location, {scan -> productScan = scan})

        return okMap().addEntity(productScan, JsonFilter.CREATE)
    }
}
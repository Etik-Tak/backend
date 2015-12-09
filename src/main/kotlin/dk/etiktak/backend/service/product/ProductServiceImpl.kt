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

package dk.etiktak.backend.service.product

import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductScan
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.location.LocationRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.product.ProductScanRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils

@Service
class ProductServiceImpl : ProductService {
    private val logger = LoggerFactory.getLogger(ProductServiceImpl::class.java)

    @Autowired
    private val productRepository: ProductRepository? = null

    @Autowired
    private val productScanRepository: ProductScanRepository? = null

    @Autowired
    private val clientRepository: ClientRepository? = null

    @Autowired
    private val locationRepository: LocationRepository? = null

    override fun getProductByUuid(uuid: String): Product? {
        return productRepository!!.findByUuid(uuid)
    }

    override fun getProductByBarcode(barcode: String): Product? {
        return productRepository!!.findByBarcode(barcode)
    }

    override fun getProductScanByUuid(uuid: String): ProductScan? {
        return productScanRepository!!.findByUuid(uuid)
    }

    /**
     * Finds a product from the given barcode and creates and returns a product scan.
     * @param barcode     Barcode
     * @param client      Client
     * @param location    Optional location
     * @return            Created product scan entry (which contains the actual product)
     */
    override fun scanProduct(barcode: String, client: Client, location: Location?): ProductScan? {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(barcode),
                "Barcode must be provided")

        Assert.notNull(
                client,
                "Client must be provided")

        // Create product scan
        val product = getProductByBarcode(barcode)
        if (product != null) {
            return createProductScan(product, client, location)
        } else {
            return null
        }
    }

    /**
     * Assigns a location to an already created product scan. Fails if location already assigned.
     * @param client         Client
     * @param productScan    Product scan entry
     * @param location       Location
     * @return               Updated product scan
     */
    override fun assignLocationToProductScan(client: Client, productScan: ProductScan, location: Location?): ProductScan {
        Assert.isTrue(
                client.uuid == productScan.client!!.uuid,
                "Client with UUID: " + client.uuid + " not owner of product scan with UUID: " + productScan.uuid)

        Assert.notNull(
                location,
                "Location must not be empty")

        Assert.isNull(
                productScan.location,
                "Location already set on product scan with UUID: " + productScan.uuid!!)

        productScan.location = location
        productScanRepository!!.save(productScan)

        return productScan
    }


    /**
     * Creates a product scan, glues it together with product and client, and saves it all.
     * @param product     Product
     * @param client      Client
     * @param location    Optional location
     * @return            Product scan
     */
    private fun createProductScan(product: Product, client: Client, location: Location?): ProductScan {
        val productScan = ProductScan()
        productScan.uuid = CryptoUtil.uuid()
        productScan.product = product
        productScan.client = client
        if (location != null) {
            productScan.location = location
        }

        client.productScans.add(productScan)

        product.productScans.add(productScan)

        if (location != null) {
            locationRepository!!.save(location)
        }
        productScanRepository!!.save(productScan)
        productRepository!!.save(product)
        clientRepository!!.save(client)

        return productScan
    }
}
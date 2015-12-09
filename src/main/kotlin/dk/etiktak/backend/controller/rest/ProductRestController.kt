package dk.etiktak.backend.controller.rest

import dk.etiktak.backend.controllers.rest.json.BaseJsonObject
import dk.etiktak.backend.controllers.rest.json.ProductJsonObject
import dk.etiktak.backend.controllers.rest.json.ProductScanJsonObject
import dk.etiktak.backend.model.product.Location
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.service.client.ClientService
import dk.etiktak.backend.service.product.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.Double

@RestController
@RequestMapping("/service/product")
class ProductRestController: BaseRestController() {

    @Autowired
    private val productService: ProductService? = null

    @Autowired
    private val clientService: ClientService? = null

    @RequestMapping(value = "/retrieve/", method = arrayOf(RequestMethod.GET))
    fun getProduct(
            @RequestParam(required = false) uuid: String?,
            @RequestParam(required = false) barcode: String?): BaseJsonObject? {
        var product: Product? = null
        if (!StringUtils.isEmpty(uuid)) {
            product = productService!!.getProductByUuid(uuid)
        }
        if (!StringUtils.isEmpty(barcode)) {
            product = productService!!.getProductByBarcode(barcode)
        }
        if (product != null) {
            return ProductJsonObject(product)
        } else {
            return BaseJsonObject(BaseJsonObject.RESULT_NOT_FOUND)
        }
    }

    @RequestMapping(value = "/scan/", method = arrayOf(RequestMethod.POST))
    fun scanProduct(
            @RequestParam barcode: String,
            @RequestParam clientUuid: String,
            @RequestParam(required = false) latitude: String?,
            @RequestParam(required = false) longitude: String?): BaseJsonObject {
        val product = productService!!.getProductByBarcode(barcode)
        if (product != null) {
            val client = clientService!!.getByUuid(clientUuid)
            var location: Location? = null
            if (!StringUtils.isEmpty(latitude) && !StringUtils.isEmpty(longitude)) {
                location = Location(Double.parseDouble(latitude), Double.parseDouble(longitude))
            }
            return ProductScanJsonObject(productService.scanProduct(barcode, client, location))
        } else {
            return BaseJsonObject(BaseJsonObject.RESULT_NOT_FOUND)
        }
    }

    @RequestMapping(value = "/scan/assign/location/", method = arrayOf(RequestMethod.POST))
    fun provideProductScanLocation(
            @RequestParam clientUuid: String,
            @RequestParam productScanUuid: String,
            @RequestParam latitude: String,
            @RequestParam longitude: String): BaseJsonObject {
        val client = clientService!!.getByUuid(clientUuid)
        val productScan = productService!!.getProductScanByUuid(productScanUuid)
        val location = Location(Double.parseDouble(latitude), Double.parseDouble(longitude))

        return ProductScanJsonObject(productService.assignLocationToProductScan(client, productScan, location))
    }
}
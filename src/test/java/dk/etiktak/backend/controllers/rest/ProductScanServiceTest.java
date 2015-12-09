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

package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.Application;
import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;
import dk.etiktak.backend.model.product.Location;
import dk.etiktak.backend.model.product.Product;
import dk.etiktak.backend.model.product.ProductScan;
import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.repository.location.LocationRepository;
import dk.etiktak.backend.repository.product.ProductRepository;
import dk.etiktak.backend.repository.product.ProductScanRepository;
import dk.etiktak.backend.repository.user.ClientRepository;
import dk.etiktak.backend.util.CryptoUtil;

import dk.etiktak.backend.util.RandomUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.util.NestedServletException;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class ProductScanServiceTest extends BaseRestTest {

    @Autowired
    private ProductScanRepository productScanRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Product product1;
    private Product product2;

    private Client client1;
    private Client client2;

    private Location location1;
    private Location location2;

    public static String serviceEndpoint(String postfix) {
        return serviceEndpoint() + "product/scan/" + postfix;
    }

    @Before
    public void setup() throws Exception {
        super.setup();

        productScanRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        clientRepository.deleteAll();

        product1 = createAndSaveProduct("123456789a", Product.BarcodeType.EAN13);
        product2 = createAndSaveProduct("123456789b", Product.BarcodeType.UPC);

        client1 = createAndSaveClient();
        client2 = createAndSaveClient();

        location1 = createAndSaveLocation();
        location2 = createAndSaveLocation();
    }

    @After
    public void tearDown() {
        productScanRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        clientRepository.deleteAll();
    }

    /**
     * Test that we can retrieve a product by scanning with location.
     */
    @Test
    public void scanProductWithLocation() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.getBarcode())
                        .param("clientUuid", client1.getUuid())
                        .param("latitude", "" + location1.getLatitude())
                        .param("longitude", "" + location1.getLongitude()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.product.uuid", is(product1.getUuid())))
                .andExpect(jsonPath("$.product.name", is(product1.getName())))
                .andExpect(jsonPath("$.product.barcode", is(product1.getBarcode())))
                .andExpect(jsonPath("$.product.barcodeType", is(product1.getBarcodeType().name())));

        validateProductScan(product1, client1, location1);
    }

    /**
     * Test that we can retrieve a product by scanning without location.
     */
    @Test
    public void scanProductWithoutLocation() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.getBarcode())
                        .param("clientUuid", client1.getUuid()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.product.uuid", is(product1.getUuid())))
                .andExpect(jsonPath("$.product.name", is(product1.getName())))
                .andExpect(jsonPath("$.product.barcode", is(product1.getBarcode())))
                .andExpect(jsonPath("$.product.barcodeType", is(product1.getBarcodeType().name())));

        validateProductScan(product1, client1);
    }

    /**
     * Test that we can assign a location to an already existant product scan.
     */
    @Test
    public void assignLocationToProductScan() throws Exception {
        ProductScan productScan = scanProduct();

        mockMvc.perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.getUuid())
                        .param("productScanUuid", productScan.getUuid())
                        .param("latitude", "" + location1.getLatitude())
                        .param("longitude", "" + location1.getLongitude()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.uuid", is(productScan.getUuid())))
                .andExpect(jsonPath("$.location.latitude", is(location1.getLatitude())))
                .andExpect(jsonPath("$.location.longitude", is(location1.getLongitude())))
                .andExpect(jsonPath("$.product.uuid", is(product1.getUuid())))
                .andExpect(jsonPath("$.product.name", is(product1.getName())))
                .andExpect(jsonPath("$.product.barcode", is(product1.getBarcode())))
                .andExpect(jsonPath("$.product.barcodeType", is(product1.getBarcodeType().name())));
    }

    /**
     * Test that we cannot assign a location to a product scan that already has a location assigned.
     */
    @Test
    public void cannotAssignLocationToProductScanWithLocationAlreadyAssigned() throws Exception {
        ProductScan productScan = scanProduct();

        // Assign first location
        mockMvc.perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.getUuid())
                        .param("productScanUuid", productScan.getUuid())
                        .param("latitude", "" + location1.getLatitude())
                        .param("longitude", "" + location1.getLongitude()))
                .andExpect(status().isOk());

        // Do it once again and fail
        exception.expect(NestedServletException.class);
        mockMvc.perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.getUuid())
                        .param("productScanUuid", productScan.getUuid())
                        .param("latitude", "" + location1.getLatitude())
                        .param("longitude", "" + location1.getLongitude()));
    }

    /**
     * Test that we cannot assign empty location to already scanned product scan.
     */
    @Test
    public void cannotAssignEmptyLocationToProductScanWithLocationAlreadyAssigned() throws Exception {
        ProductScan productScan = scanProduct();

        mockMvc.perform(
                post(serviceEndpoint("assign/location/"))
                        .param("clientUuid", client1.getUuid())
                        .param("productScanUuid", productScan.getUuid()))
                .andExpect(status().is(400));
    }



    private ProductScan scanProduct() throws Exception {
        mockMvc.perform(
                post(serviceEndpoint(""))
                        .param("barcode", product1.getBarcode())
                        .param("clientUuid", client1.getUuid()))
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.location", nullValue()));

        List<ProductScan> productScans = productScanRepository.findByProductUuid(product1.getUuid());
        return productScans.get(0);
    }

    private void validateProductScan(Product product, Client client) {
        validateProductScan(product, client, null);
    }

    private void validateProductScan(Product product, Client client, Location location) {
        List<ProductScan> productScansFromProduct = productScanRepository.findByProductUuid(product.getUuid());
        Assert.notEmpty(productScansFromProduct, "Did not find product scan for product with uuid: " + product.getUuid());
        Assert.isTrue(productScansFromProduct.size() == 1, "More than one product scan found for product with uuid: " + product.getUuid());
        validateProductScan(productScansFromProduct.get(0), product, client, location);

        List<ProductScan> productScansFromClient = productScanRepository.findByClientUuid(client.getUuid());
        Assert.notEmpty(productScansFromClient, "Did not find product scan for client with uuid: " + client.getUuid());
        Assert.isTrue(productScansFromClient.size() == 1, "More than one product scan found for client with uuid: " + client.getUuid());
        validateProductScan(productScansFromClient.get(0), product, client, location);
    }

    private void validateProductScan(ProductScan productScan, Product product, Client client, Location location) {
        Assert.isTrue(productScan.getProduct().getUuid().equals(product.getUuid()), "Product scan's product was not the product expected!");
        Assert.isTrue(productScan.getClient().getUuid().equals(client.getUuid()), "Product scan's client was not the client expected!");
        if (location != null && productScan.getLocation() != null) {
            Assert.isTrue(productScan.getLocation().getLatitude() == location.getLatitude(), "Latitude for product scan not correct");
            Assert.isTrue(productScan.getLocation().getLongitude() == location.getLongitude(), "Longitude for product scan not correct");
        } else {
            Assert.isNull(productScan.getLocation(), "Location for product scan was expected to be null, but was not!");
        }
    }

    private Product createAndSaveProduct(String barcode, Product.BarcodeType barcodeType) {
        Product product = new Product();
        product.setUuid(CryptoUtil.uuid());
        product.setName(CryptoUtil.uuid());
        product.setBarcode(barcode);
        product.setBarcodeType(barcodeType);
        productRepository.save(product);
        return product;
    }

    private Client createAndSaveClient() {
        Client client = new Client();
        client.setUuid(CryptoUtil.uuid());
        client.setVerified(false);
        clientRepository.save(client);
        return client;
    }

    private Location createAndSaveLocation() {
        Location location = new Location();
        location.setLatitude(RandomUtil.getValueWithScale(Math.random(), 6));
        location.setLongitude(RandomUtil.getValueWithScale(Math.random(), 6));
        locationRepository.save(location);
        return location;
    }
}

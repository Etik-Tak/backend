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
import dk.etiktak.backend.model.product.Product;
import dk.etiktak.backend.repository.ProductRepository;
import dk.etiktak.backend.util.CryptoUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class ProductRetrievalServiceTest extends BaseRestTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;

    public static String serviceEndpoint(String postfix) {
        return serviceEndpoint() + "product/retrieve/" + postfix;
    }

    @Before
    public void setup() throws Exception {
        super.setup();

        productRepository.deleteAll();

        product1 = createAndSaveProduct("123456789a", Product.BarcodeType.EAN13);
        product2 = createAndSaveProduct("123456789b", Product.BarcodeType.UPC);
    }

    /**
     * Test that we can retrieve product by UUID.
     */
    @Test
    public void retrieveProductByUuid() throws Exception {
        mockMvc.perform(
                get(serviceEndpoint(""))
                        .param("uuid", product1.getUuid()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.uuid", is(product1.getUuid())))
                .andExpect(jsonPath("$.barcode", is(product1.getBarcode())))
                .andExpect(jsonPath("$.barcodeType", is(product1.getBarcodeType().name())));
    }

    /**
     * Test that we can retrieve product by EAN13 barcode.
     */
    @Test
    public void retrieveProductByEan13Barcode() throws Exception {
        mockMvc.perform(
                get(serviceEndpoint(""))
                        .param("barcode", product1.getBarcode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.uuid", is(product1.getUuid())))
                .andExpect(jsonPath("$.barcode", is(product1.getBarcode())))
                .andExpect(jsonPath("$.barcodeType", is(product1.getBarcodeType().name())));
    }

    /**
     * Test that we can retrieve product by UPC barcode.
     */
    @Test
    public void retrieveProductByUPCBarcode() throws Exception {
        mockMvc.perform(
                get(serviceEndpoint(""))
                        .param("barcode", product2.getBarcode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(jsonContentType))
                .andExpect(jsonPath("$.result", is(BaseJsonObject.RESULT_OK)))
                .andExpect(jsonPath("$.uuid", is(product2.getUuid())))
                .andExpect(jsonPath("$.barcode", is(product2.getBarcode())))
                .andExpect(jsonPath("$.barcodeType", is(product2.getBarcodeType().name())));
    }

    private Product createAndSaveProduct(String barcode, Product.BarcodeType barcodeType) {
        Product product = new Product();
        product.setUuid(CryptoUtil.uuid());
        product.setBarcode(barcode);
        product.setBarcodeType(barcodeType);
        productRepository.save(product);
        return product;
    }
}

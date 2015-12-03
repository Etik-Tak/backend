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

package dk.etiktak.backend.service.product;

import dk.etiktak.backend.model.product.Location;
import dk.etiktak.backend.model.product.Product;
import dk.etiktak.backend.model.product.ProductScan;
import dk.etiktak.backend.model.user.Client;
import dk.etiktak.backend.repository.location.LocationRepository;
import dk.etiktak.backend.repository.product.ProductRepository;
import dk.etiktak.backend.repository.product.ProductScanRepository;
import dk.etiktak.backend.repository.user.ClientRepository;

import dk.etiktak.backend.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ProductServiceImpl implements ProductService {
    private Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductScanRepository productScanRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Override
    public Product getProductByUuid(String uuid) {
        return productRepository.findByUuid(uuid);
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    @Override
    public ProductScan getProductScanByUuid(String uuid) {
        return productScanRepository.findByUuid(uuid);
    }

    @Override
    public ProductScan scanProduct(String barcode, Client client, Location location) {
        ProductScan productScan = null;
        Product product = getProductByBarcode(barcode);
        if (product != null) {
            productScan = createProductScan(product, client, location);
        }
        return productScan;
    }

    @Override
    public ProductScan assignLocationToProductScan(Client client, ProductScan productScan, Location location) {
        Assert.isTrue(
                client.getUuid().equals(productScan.getClient().getUuid()),
                "Client with UUID: " + client.getUuid() + " not owner of product scan with UUID: " + productScan.getUuid());

        Assert.notNull(
                location,
                "Location must not be empty");

        Assert.isNull(
                productScan.getLocation(),
                "Location already set on product scan with UUID: " + productScan.getUuid()
        );

        productScan.setLocation(location);
        productScanRepository.save(productScan);

        return productScan;
    }


    private ProductScan createProductScan(Product product, Client client, Location location) {
        ProductScan productScan = new ProductScan();
        productScan.setUuid(CryptoUtil.uuid());
        productScan.setProduct(product);
        productScan.setClient(client);
        if (location != null) {
            productScan.setLocation(location);
        }

        client.getProductScans().add(productScan);

        product.getProductScans().add(productScan);

        if (location != null) {
            locationRepository.save(location);
        }
        productScanRepository.save(productScan);
        productRepository.save(product);
        clientRepository.save(client);

        return productScan;
    }
}

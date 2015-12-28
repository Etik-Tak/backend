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

import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProductCategoryServiceImpl @Autowired constructor(
        private val productCategoryRepository: ProductCategoryRepository,
        private val clientRepository: ClientRepository) : ProductCategoryService {

    private val logger = LoggerFactory.getLogger(ProductCategoryServiceImpl::class.java)

    /**
     * Finds a product category from the given UUID.
     *
     * @param uuid  UUID
     * @return      Product category with given UUID
     */
    override fun getProductCategoryByUuid(uuid: String): ProductCategory? {
        return productCategoryRepository.findByUuid(uuid)
    }

    /**
     * Creates a product category.
     *
     * @param name          Name
     * @param modifyValues  Function called with modified client
     * @return              Product category
     */
    override fun createProductCategory(client: Client, name: String, modifyValues: (Client) -> Unit): ProductCategory {
        val productCategory = ProductCategory()
        productCategory.uuid = CryptoUtil().uuid()
        productCategory.creator = client
        productCategory.name = name

        client.productCategories.add(productCategory)

        val modifiedClient = clientRepository.save(client)
        val modifiedProductCategory = productCategoryRepository.save(productCategory)

        modifyValues(modifiedClient)

        return modifiedProductCategory
    }
}
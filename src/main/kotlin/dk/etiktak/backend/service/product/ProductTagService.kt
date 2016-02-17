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

import dk.etiktak.backend.model.product.ProductTag
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.product.ProductTagRepository
import dk.etiktak.backend.service.security.ClientVerified
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class ProductTagService @Autowired constructor(
        private val productTagRepository: ProductTagRepository) {

    private val logger = LoggerFactory.getLogger(ProductLabelService::class.java)

    /**
     * Finds a product tag from the given UUID.
     *
     * @param uuid  UUID
     * @return      Product tag with given UUID
     */
    open fun getProductTagByUuid(uuid: String): ProductTag? {
        return productTagRepository.findByUuid(uuid)
    }

    /**
     * Creates a product tag.
     *
     * @param name     Name
     * @return         Product tag
     */
    @ClientVerified
    open fun createProductTag(client: Client, name: String): ProductTag {
        val productTag = ProductTag()
        productTag.uuid = CryptoUtil().uuid()
        productTag.name = name

        val modifiedProductTag = productTagRepository.save(productTag)

        return modifiedProductTag
    }
}
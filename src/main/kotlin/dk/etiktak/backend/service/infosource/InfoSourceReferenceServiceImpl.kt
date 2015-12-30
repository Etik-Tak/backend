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

package dk.etiktak.backend.service.infosource

import dk.etiktak.backend.model.infochannel.InfoChannel
import dk.etiktak.backend.model.infosource.InfoSource
import dk.etiktak.backend.model.infosource.InfoSourceReference
import dk.etiktak.backend.model.product.Product
import dk.etiktak.backend.model.product.ProductCategory
import dk.etiktak.backend.model.product.ProductLabel
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.infochannel.InfoChannelRepository
import dk.etiktak.backend.repository.infosource.InfoSourceReferenceRepository
import dk.etiktak.backend.repository.infosource.InfoSourceRepository
import dk.etiktak.backend.repository.product.ProductCategoryRepository
import dk.etiktak.backend.repository.product.ProductLabelRepository
import dk.etiktak.backend.repository.product.ProductRepository
import dk.etiktak.backend.repository.user.ClientRepository
import dk.etiktak.backend.service.infochannel.InfoChannelService
import dk.etiktak.backend.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.util.*

@Service
class InfoSourceReferenceServiceImpl @Autowired constructor(
        private val infoChannelService: InfoChannelService,
        private val infoSourceReferenceRepository: InfoSourceReferenceRepository,
        private val infoSourceRepository: InfoSourceRepository,
        private val infoChannelRepository: InfoChannelRepository,
        private val clientRepository: ClientRepository,
        private val productRepository: ProductRepository,
        private val productCategoryRepository: ProductCategoryRepository,
        private val productLabelRepository: ProductLabelRepository) : InfoSourceReferenceService {

    private val logger = LoggerFactory.getLogger(InfoSourceReferenceServiceImpl::class.java)

    /**
     * Finds an info source reference from the given UUID.
     *
     * @param uuid  UUID
     * @return      Info source reference with given UUID
     */
    override fun getInfoSourceReferenceByUuid(uuid: String): InfoSourceReference? {
        return infoSourceReferenceRepository.findByUuid(uuid)
    }

    /**
     * Creates an info source reference.
     *
     * @param client           Creator
     * @param infoChannel      Info channel to attach
     * @param infoSource       Info source from which it is based
     * @param url              Reference URL
     * @param title            Title of reference
     * @param summary          Summary
     * @param modifyValues     Function called with modified client, info channel and info source
     * @return                 Created info source reference
     */
    override fun createInfoSourceReference(client: Client, infoChannel: InfoChannel, infoSource: InfoSource,
                                           url: String, title: String, summary: String,
                                           modifyValues: (Client, InfoChannel, InfoSource) -> Unit): InfoSourceReference {

        // Check for empty fields
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(title),
                "Title must be provided")

        Assert.isTrue(
                !StringUtils.isEmpty(summary),
                "Summary must be provided")

        // Validate url
        validateUrlReference(url, infoSource)

        logger.info("Creating new info source reference with title: $title")

        // Create info source
        val infoSourceReference = InfoSourceReference()
        infoSourceReference.uuid = CryptoUtil().uuid()
        infoSourceReference.url = url
        infoSourceReference.title = title
        infoSourceReference.summary = summary
        infoSourceReference.creator = client
        infoSourceReference.infoSource = infoSource
        infoSourceReference.infoChannel = infoChannel

        // Glue it all together
        client.infoSourceReferences.add(infoSourceReference)
        infoChannel.infoSourceReferences.add(infoSourceReference)
        infoSource.infoSourceReferences.add(infoSourceReference)

        // Save it all
        val modifiedInfoSourceReference = infoSourceReferenceRepository.save(infoSourceReference)
        val modifiedClient = clientRepository.save(client)
        val modifiedInfoSource = infoSourceRepository.save(infoSource)
        val modifiedInfoChannel = infoChannelRepository.save(infoChannel)

        modifyValues(modifiedClient, modifiedInfoChannel, modifiedInfoSource)

        return modifiedInfoSourceReference
    }

    /**
     * Assigns products to an info source reference.
     *
     * @param client                Client
     * @param infoSourceReference   Info source reference
     * @param products              Products
     * @param modifyValues          Function called with modified info source reference and products
     */
    override fun assignProductsToInfoSourceReference(client: Client, infoSourceReference: InfoSourceReference, products: List<Product>,
                                                     modifyValues: (InfoSourceReference, List<Product>) -> Unit) {

        // Check for empty fields
        Assert.isTrue(
                products.size > 0,
                "Products must be provided")

        // Validate ownership to info channel
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoSourceReference.infoChannel),
                "Client not member of info channel")

        // Assign products to info source reference
        infoSourceReference.products.addAll(products)
        for (product in products) {
            product.infoSourceReferences.add(infoSourceReference)
        }

        // Save it all
        val modifiedInfoSourceReference = infoSourceReferenceRepository.save(infoSourceReference)
        val modifiedProducts: MutableList<Product> = ArrayList()
        for (product in products) {
            modifiedProducts.add(productRepository.save(product))
        }

        modifyValues(modifiedInfoSourceReference, modifiedProducts)
    }

    /**
     * Assigns product categories to an info source reference.
     *
     * @param client                Client
     * @param infoSourceReference   Info source reference
     * @param productCategories     Product categories
     * @param modifyValues          Function called with modified info source reference and product categories
     */
    override fun assignProductCategoriesToInfoSourceReference(client: Client, infoSourceReference: InfoSourceReference, productCategories: List<ProductCategory>,
                                                              modifyValues: (InfoSourceReference, List<ProductCategory>) -> Unit) {

        // Check for empty fields
        Assert.isTrue(
                productCategories.size > 0,
                "Product categories must be provided")

        // Validate ownership to info channel
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoSourceReference.infoChannel),
                "Client not member of info channel")

        // Assign product categories to info source reference
        infoSourceReference.productCategories.addAll(productCategories)
        for (productCategory in productCategories) {
            productCategory.infoSourceReferences.add(infoSourceReference)
        }

        // Save it all
        val modifiedInfoSourceReference = infoSourceReferenceRepository.save(infoSourceReference)
        val modifiedProductCategories: MutableList<ProductCategory> = ArrayList()
        for (productCategory in productCategories) {
            modifiedProductCategories.add(productCategoryRepository.save(productCategory))
        }

        modifyValues(modifiedInfoSourceReference, modifiedProductCategories)
    }

    /**
     * Assigns product labels to an info source reference.
     *
     * @param client                Client
     * @param infoSourceReference   Info source reference
     * @param productLabels         Product labels
     * @param modifyValues          Function called with modified info source reference and product labels
     */
    override fun assignProductLabelsToInfoSourceReference(client: Client, infoSourceReference: InfoSourceReference, productLabels: List<ProductLabel>,
                                                          modifyValues: (InfoSourceReference, List<ProductLabel>) -> Unit) {

        // Check for empty fields
        Assert.isTrue(
                productLabels.size > 0,
                "Product labels must be provided")

        // Validate ownership to info channel
        Assert.isTrue(
                infoChannelService.isClientMemberOfInfoChannel(client, infoSourceReference.infoChannel),
                "Client not member of info channel")

        // Assign product labels to info source reference
        infoSourceReference.productLabels.addAll(productLabels)
        for (productLabel in productLabels) {
            productLabel.infoSourceReferences.add(infoSourceReference)
        }

        // Save it all
        val modifiedInfoSourceReference = infoSourceReferenceRepository.save(infoSourceReference)
        val modifiedProductLabels: MutableList<ProductLabel> = ArrayList()
        for (productLabel in productLabels) {
            modifiedProductLabels.add(productLabelRepository.save(productLabel))
        }

        modifyValues(modifiedInfoSourceReference, modifiedProductLabels)
    }

    /**
     * Validates an info source reference url.
     *
     * @param url         Url
     * @param infoSource  Info source
     * @throws            Exception if url does not validate
     */
    private fun validateUrlReference(url: String, infoSource: InfoSource) {
        // TODO! Implement url prefix validator!
        Assert.isTrue(
                !StringUtils.isEmpty(url),
                "URL must not be empty")

        var didMatchAny = false
        for (urlPrefix in infoSource.urlPrefixes) {
            didMatchAny = didMatchAny || url.toLowerCase().startsWith(urlPrefix.urlPrefix)
        }
        Assert.isTrue(
                didMatchAny,
                "URL must be from the given info source, e.g. it must start with fx. " + infoSource.urlPrefixes.first().urlPrefix + ", but was: " + url
        )
    }
}
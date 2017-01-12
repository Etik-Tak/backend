// Copyright (c) 2017, Daniel Andersen (daniel@trollsahead.dk)
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

package dk.etiktak.backend.service.changelog

import dk.etiktak.backend.model.changelog.ChangeLog
import dk.etiktak.backend.model.changelog.ChangeType
import dk.etiktak.backend.model.changelog.ChangedEntityType
import dk.etiktak.backend.model.user.Client
import dk.etiktak.backend.repository.changelog.ChangeLogRepository
import dk.etiktak.backend.repository.user.ClientRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class ChangeLogService @Autowired constructor(
        private val changeLogRepository: ChangeLogRepository,
        private val clientRepository: ClientRepository) {

    private val logger = LoggerFactory.getLogger(ChangeLogService::class.java)

    /**
     * Creates a change log.
     *
     * @param client             Client
     * @param changedEntityType  Type of changed entity
     * @param entityUuid         Uuid of entity
     * @param changeType         Type of change
     * @param changeText         Change text
     * @param modifyValues       Function called with modified client
     * @return                   Change log entry
     */
    open fun createChangeLog(client: Client, changedEntityType: ChangedEntityType, entityUuid: String?, changeType: ChangeType, changeText: String?,
                             modifyValues: (Client) -> Unit = {}): ChangeLog? {

        val changeLog = ChangeLog()
        changeLog.client = client
        changeLog.changedEntityType = changedEntityType
        changeLog.entityUuid = entityUuid
        changeLog.changeType = changeType
        changeLog.changeText = changeText

        client.changeLogs.add(changeLog)

        val modifiedChangeLog = changeLogRepository.save(changeLog)
        val modifiedClient = clientRepository.save(client)

        modifyValues(modifiedClient)

        return modifiedChangeLog
    }
}
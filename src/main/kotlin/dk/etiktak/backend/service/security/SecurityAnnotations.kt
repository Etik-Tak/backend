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

package dk.etiktak.backend.service.security

import dk.etiktak.backend.model.user.Client
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientVerified

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientValid



@Component
@Configuration
@EnableAspectJAutoProxy
@Aspect
open class ClientVerifiedAspect {

    @Autowired
    private val securityService: SecurityService? = null

    @Before(value = "@within(dk.etiktak.backend.service.security.ClientVerified) || @annotation(dk.etiktak.backend.service.security.ClientVerified)")
    fun before(joinPoint: JoinPoint) {
        for (argument in joinPoint.args) {
            argument?.let {
                if (argument.javaClass == Client::class.java) {
                    securityService!!.assertClientVerified(argument as Client)
                }
            }
        }
    }
}

@Component
@Configuration
@EnableAspectJAutoProxy
@Aspect
open class ClientValidAspect {

    @Autowired
    private val securityService: SecurityService? = null

    @Before(value = "@within(dk.etiktak.backend.service.security.ClientValid) || @annotation(dk.etiktak.backend.service.security.ClientValid)")
    fun before(joinPoint: JoinPoint) {
        for (argument in joinPoint.args) {
            argument?.let {
                if (argument.javaClass == Client::class.java) {
                    securityService!!.assertClientValid(argument as Client)
                }
            }
        }
    }
}

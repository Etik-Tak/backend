package dk.etiktak.backend.security

import dk.etiktak.backend.model.acl.AclRole
import org.springframework.security.core.GrantedAuthority

open class AuthenticationAuthority constructor(
        private val role: AclRole): GrantedAuthority {

    override fun getAuthority(): String? {
        return role.name
    }
}
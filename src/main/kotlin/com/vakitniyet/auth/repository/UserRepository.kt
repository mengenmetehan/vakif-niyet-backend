package com.vakitniyet.auth.repository

import com.vakitniyet.auth.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByAppleId(appleId: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
}

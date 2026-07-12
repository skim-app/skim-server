package com.uson.skim.server.recording

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RecordingRepository : JpaRepository<RecordingEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedById(id: UUID): RecordingEntity?
}

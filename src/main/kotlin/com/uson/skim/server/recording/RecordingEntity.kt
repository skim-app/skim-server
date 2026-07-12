package com.uson.skim.server.recording

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "recordings")
class RecordingEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var title: String,
    @Column(name = "duration_ms") var durationMs: Long? = null,
    @Column(nullable = false) var status: String = "UPLOADED",
    @Column(name = "failure_reason") var failureReason: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Column(name = "audio_storage_key") var audioStorageKey: String? = null,
    @Column(name = "audio_content_type") var audioContentType: String? = null,
)

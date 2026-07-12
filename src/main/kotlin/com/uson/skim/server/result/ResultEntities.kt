package com.uson.skim.server.result

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transcript_chunks")
class TranscriptChunkEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "recording_id", nullable = false) val recordingId: UUID,
    @Column(name = "start_ms", nullable = false) val startMs: Long,
    @Column(name = "end_ms", nullable = false) val endMs: Long,
    @Column(nullable = false) val text: String,
    @Column(name = "sequence_number", nullable = false) val sequenceNumber: Int,
)

@Entity
@Table(name = "summary_items")
class SummaryItemEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "recording_id", nullable = false) val recordingId: UUID,
    @Column(nullable = false) val category: String,
    @Column(nullable = false) val text: String,
)

@Entity
@Table(name = "summary_sources")
class SummarySourceEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "summary_item_id", nullable = false) val summaryItemId: UUID,
    @Column(name = "transcript_chunk_id", nullable = false) val transcriptChunkId: UUID,
    @Column(name = "start_ms", nullable = false) val startMs: Long,
    @Column(name = "end_ms", nullable = false) val endMs: Long,
)

@Entity
@Table(name = "todos")
class TodoEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "recording_id", nullable = false) val recordingId: UUID,
    @Column(name = "summary_item_id") val summaryItemId: UUID? = null,
    @Column(nullable = false) var title: String,
    @Column(name = "is_completed", nullable = false) var isCompleted: Boolean = false,
    @Column(name = "source_start_ms") val sourceStartMs: Long? = null,
    @Column(name = "source_end_ms") val sourceEndMs: Long? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "processing_jobs")
class ProcessingJobEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "recording_id", nullable = false) val recordingId: UUID,
    @Column(nullable = false) var status: String,
    @Column(name = "failure_reason") var failureReason: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

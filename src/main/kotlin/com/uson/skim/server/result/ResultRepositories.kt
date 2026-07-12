package com.uson.skim.server.result

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TranscriptChunkRepository : JpaRepository<TranscriptChunkEntity, UUID> {
    fun findAllByRecordingIdOrderBySequenceNumber(recordingId: UUID): List<TranscriptChunkEntity>
}

interface SummaryItemRepository : JpaRepository<SummaryItemEntity, UUID> {
    fun findAllByRecordingId(recordingId: UUID): List<SummaryItemEntity>
}

interface SummarySourceRepository : JpaRepository<SummarySourceEntity, UUID> {
    fun findAllBySummaryItemIdIn(summaryItemIds: Collection<UUID>): List<SummarySourceEntity>
}

interface TodoRepository : JpaRepository<TodoEntity, UUID> {
    fun findAllByRecordingIdOrderByCreatedAtDesc(recordingId: UUID): List<TodoEntity>
}

interface ProcessingJobRepository : JpaRepository<ProcessingJobEntity, UUID> {
    fun findByRecordingId(recordingId: UUID): ProcessingJobEntity?
}

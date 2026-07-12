package com.uson.skim.server.recording

import com.uson.skim.server.result.ProcessingJobRepository
import com.uson.skim.server.result.SummaryItemEntity
import com.uson.skim.server.result.SummaryItemRepository
import com.uson.skim.server.result.SummarySourceEntity
import com.uson.skim.server.result.SummarySourceRepository
import com.uson.skim.server.result.TranscriptChunkEntity
import com.uson.skim.server.result.TranscriptChunkRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.util.UUID

data class ProcessingRequested(val recordingId: UUID)

@Component
class DeterministicProcessingWorker(
    private val states: ProcessingStateService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun process(event: ProcessingRequested) {
        try {
            states.markSummarizing(event.recordingId)
            states.complete(event.recordingId)
        } catch (exception: Exception) {
            states.fail(event.recordingId, exception.message ?: "Processing failed")
        }
    }
}

@Service
class ProcessingStateService(
    private val recordings: RecordingRepository,
    private val jobs: ProcessingJobRepository,
    private val chunks: TranscriptChunkRepository,
    private val summaries: SummaryItemRepository,
    private val sources: SummarySourceRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markSummarizing(recordingId: UUID) {
        val recording = recordings.findLockedById(recordingId) ?: return
        if (recording.status != TRANSCRIBING) return
        recording.status = SUMMARIZING
        recording.updatedAt = Instant.now()
        jobs.findByRecordingId(recordingId)?.let { job ->
            job.status = SUMMARIZING
            job.updatedAt = recording.updatedAt
            jobs.save(job)
        }
        recordings.save(recording)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun complete(recordingId: UUID) {
        val recording = recordings.findLockedById(recordingId) ?: return
        if (recording.status != SUMMARIZING) return
        val durationMs = recording.durationMs ?: DEFAULT_DURATION_MS
        val chunk = chunks.save(TranscriptChunkEntity(
            recordingId = recording.id,
            startMs = 0,
            endMs = durationMs,
            text = "업로드한 음성 메모를 처리했습니다. 요약의 근거 구간을 재생해 확인하세요.",
            sequenceNumber = 0,
        ))
        val summary = summaries.save(SummaryItemEntity(
            recordingId = recording.id,
            category = "핵심",
            text = "업로드한 음성 메모의 근거를 timestamp로 확인할 수 있습니다.",
        ))
        sources.save(SummarySourceEntity(summaryItemId = summary.id, transcriptChunkId = chunk.id, startMs = chunk.startMs, endMs = chunk.endMs))
        recording.durationMs = durationMs
        recording.status = COMPLETED
        recording.updatedAt = Instant.now()
        jobs.findByRecordingId(recordingId)?.let { job ->
            job.status = COMPLETED
            job.updatedAt = recording.updatedAt
            jobs.save(job)
        }
        recordings.save(recording)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun fail(recordingId: UUID, reason: String) {
        val recording = recordings.findLockedById(recordingId) ?: return
        if (recording.status == COMPLETED) return
        recording.status = FAILED
        recording.failureReason = reason.take(MAX_FAILURE_REASON_LENGTH)
        recording.updatedAt = Instant.now()
        jobs.findByRecordingId(recordingId)?.let { job ->
            job.status = FAILED
            job.failureReason = recording.failureReason
            job.updatedAt = recording.updatedAt
            jobs.save(job)
        }
        recordings.save(recording)
    }

    private companion object {
        const val TRANSCRIBING = "TRANSCRIBING"
        const val SUMMARIZING = "SUMMARIZING"
        const val COMPLETED = "COMPLETED"
        const val FAILED = "FAILED"
        const val DEFAULT_DURATION_MS = 6_000L
        const val MAX_FAILURE_REASON_LENGTH = 500
    }
}

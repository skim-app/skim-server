package com.uson.skim.server.recording

import com.uson.skim.server.result.SummaryItemEntity
import com.uson.skim.server.result.SummaryItemRepository
import com.uson.skim.server.result.SummarySourceEntity
import com.uson.skim.server.result.SummarySourceRepository
import com.uson.skim.server.result.TranscriptChunkEntity
import com.uson.skim.server.result.TranscriptChunkRepository
import com.uson.skim.server.result.ProcessingJobEntity
import com.uson.skim.server.result.ProcessingJobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
class AudioWorkflowService(
    private val recordings: RecordingRepository,
    private val chunks: TranscriptChunkRepository,
    private val summaries: SummaryItemRepository,
    private val sources: SummarySourceRepository,
    private val jobs: ProcessingJobRepository,
    private val events: ApplicationEventPublisher,
    @Value("\${skim.storage-directory:\${java.io.tmpdir}/skim-audio}") private val storageDirectory: String,
) {
    fun store(recording: RecordingEntity, file: MultipartFile): RecordingEntity {
        require(file.size in 1..MAX_AUDIO_BYTES) { "Audio file must be between 1 byte and 10 MB" }
        require(file.contentType?.startsWith("audio/") == true) { "Only audio files are supported" }
        val directory = Path.of(storageDirectory)
        Files.createDirectories(directory)
        val key = "${recording.id}-${file.originalFilename?.substringAfterLast('.', "audio") ?: "audio"}"
        file.inputStream.use { input -> Files.copy(input, directory.resolve(key), StandardCopyOption.REPLACE_EXISTING) }
        recording.audioStorageKey = key
        recording.audioContentType = file.contentType
        recording.status = "UPLOADED"
        recording.failureReason = null
        recording.updatedAt = java.time.Instant.now()
        return recordings.save(recording)
    }

    @Transactional
    fun start(recordingId: java.util.UUID): RecordingEntity {
        val recording = recordings.findLockedById(recordingId) ?: throw NoSuchElementException("Recording not found")
        requireNotNull(recording.audioStorageKey) { "Upload audio before processing" }
        if (recording.status in terminalOrActiveStatuses) return recording
        val job = jobs.findByRecordingId(recording.id) ?: ProcessingJobEntity(recordingId = recording.id, status = "UPLOADED")
        recording.status = "TRANSCRIBING"
        recording.updatedAt = java.time.Instant.now()
        job.status = "TRANSCRIBING"
        job.failureReason = null
        job.updatedAt = recording.updatedAt
        jobs.save(job)
        val saved = recordings.save(recording)
        events.publishEvent(ProcessingRequested(saved.id))
        return saved
    }

    fun audioPath(recording: RecordingEntity): Path = Path.of(storageDirectory).resolve(requireNotNull(recording.audioStorageKey) { "Audio not found" })

    private companion object {
        const val MAX_AUDIO_BYTES = 10L * 1024 * 1024
        val terminalOrActiveStatuses = setOf("TRANSCRIBING", "SUMMARIZING", "COMPLETED")
    }
}

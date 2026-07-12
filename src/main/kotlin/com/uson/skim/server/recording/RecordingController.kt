package com.uson.skim.server.recording

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

data class CreateRecordingRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
)

data class RecordingResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val durationMs: Long?,
    val createdAt: Instant,
    val audioAvailable: Boolean,
)

@RestController
@RequestMapping("/v1/recordings")
class RecordingController(
    private val recordings: RecordingRepository,
    private val workflow: AudioWorkflowService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateRecordingRequest): RecordingResponse {
        val entity = recordings.save(RecordingEntity(title = request.title.trim()))
        return entity.toResponse()
    }

    @GetMapping
    fun list(): List<RecordingResponse> = recordings.findAll().sortedByDescending { it.createdAt }.map { it.toResponse() }

    @GetMapping("/{recordingId}")
    fun get(@PathVariable recordingId: UUID): RecordingResponse = recordings.findById(recordingId)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found") }
        .toResponse()

    @PostMapping("/{recordingId}/audio")
    fun upload(@PathVariable recordingId: UUID, @RequestPart file: MultipartFile): RecordingResponse = try {
        workflow.store(recording(recordingId), file).toResponse()
    } catch (exception: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, exception.message, exception)
    }

    @PostMapping("/{recordingId}/process")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun process(@PathVariable recordingId: UUID): RecordingResponse = try {
        workflow.start(recording(recordingId).id).toResponse()
    } catch (exception: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.CONFLICT, exception.message, exception)
    }

    @GetMapping("/{recordingId}/audio")
    fun audio(@PathVariable recordingId: UUID): ResponseEntity<FileSystemResource> {
        val recording = recording(recordingId)
        val path = try { workflow.audioPath(recording) } catch (exception: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, exception.message, exception)
        }
        if (!java.nio.file.Files.exists(path)) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found")
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .contentType(MediaType.parseMediaType(recording.audioContentType ?: "application/octet-stream"))
            .contentLength(java.nio.file.Files.size(path))
            .body(FileSystemResource(path))
    }

    private fun recording(id: UUID): RecordingEntity = recordings.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found") }
}

private fun RecordingEntity.toResponse() = RecordingResponse(id, title, status, durationMs, createdAt, audioStorageKey != null)

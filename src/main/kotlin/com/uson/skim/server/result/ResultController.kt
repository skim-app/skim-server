package com.uson.skim.server.result

import com.uson.skim.server.recording.RecordingRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

data class ProcessingStatusResponse(val status: String, val failureReason: String?, val updatedAt: Instant)
data class TranscriptChunkResponse(val id: UUID, val startMs: Long, val endMs: Long, val text: String, val sequence: Int)
data class TranscriptResponse(val recordingId: UUID, val chunks: List<TranscriptChunkResponse>)
data class SummarySourceResponse(val transcriptChunkId: UUID, val startMs: Long, val endMs: Long, val label: String)
data class SummaryItemResponse(val id: UUID, val category: String, val text: String, val sources: List<SummarySourceResponse>)
data class SummaryResponse(val recordingId: UUID, val items: List<SummaryItemResponse>)
data class CreateTodoRequest(
    @field:NotNull val recordingId: UUID,
    val summaryItemId: UUID? = null,
    @field:NotBlank val title: String,
    val sourceStartMs: Long? = null,
    val sourceEndMs: Long? = null,
)
data class UpdateTodoRequest(@field:NotNull val isCompleted: Boolean)
data class TodoResponse(
    val id: UUID,
    val recordingId: UUID,
    val summaryItemId: UUID?,
    val title: String,
    val isCompleted: Boolean,
    val sourceStartMs: Long?,
    val sourceEndMs: Long?,
    val createdAt: Instant,
)

@RestController
@RequestMapping("/v1")
class ResultController(
    private val recordings: RecordingRepository,
    private val chunks: TranscriptChunkRepository,
    private val summaries: SummaryItemRepository,
    private val sources: SummarySourceRepository,
    private val todos: TodoRepository,
) {
    @GetMapping("/recordings/{recordingId}/processing-status")
    fun processingStatus(@PathVariable recordingId: UUID): ProcessingStatusResponse {
        val recording = recording(recordingId)
        return ProcessingStatusResponse(recording.status, recording.failureReason, recording.updatedAt)
    }

    @GetMapping("/recordings/{recordingId}/transcript")
    fun transcript(@PathVariable recordingId: UUID): TranscriptResponse {
        recording(recordingId)
        return TranscriptResponse(recordingId, chunks.findAllByRecordingIdOrderBySequenceNumber(recordingId).map {
            TranscriptChunkResponse(it.id, it.startMs, it.endMs, it.text, it.sequenceNumber)
        })
    }

    @GetMapping("/recordings/{recordingId}/summary")
    fun summary(@PathVariable recordingId: UUID): SummaryResponse {
        recording(recordingId)
        val items = summaries.findAllByRecordingId(recordingId)
        val sourceByItem = sources.findAllBySummaryItemIdIn(items.map { it.id }).groupBy { it.summaryItemId }
        return SummaryResponse(recordingId, items.map { item ->
            SummaryItemResponse(item.id, item.category, item.text, sourceByItem[item.id].orEmpty().map { source ->
                SummarySourceResponse(source.transcriptChunkId, source.startMs, source.endMs, label(source.startMs, source.endMs))
            })
        })
    }

    @PostMapping("/todos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createTodo(@Valid @RequestBody request: CreateTodoRequest): TodoResponse {
        validateTimePair(request.sourceStartMs, request.sourceEndMs)
        recording(request.recordingId)
        request.summaryItemId?.let { summaryId ->
            val summary = summaries.findById(summaryId).orElseThrow { notFound("Summary item") }
            if (summary.recordingId != request.recordingId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary item belongs to another recording")
        }
        return todos.save(TodoEntity(
            recordingId = request.recordingId,
            summaryItemId = request.summaryItemId,
            title = request.title.trim(),
            sourceStartMs = request.sourceStartMs,
            sourceEndMs = request.sourceEndMs,
        )).toResponse()
    }

    @GetMapping("/todos")
    fun listTodos(@RequestParam recordingId: UUID): List<TodoResponse> {
        recording(recordingId)
        return todos.findAllByRecordingIdOrderByCreatedAtDesc(recordingId).map { it.toResponse() }
    }

    @PatchMapping("/todos/{todoId}")
    @Transactional
    fun updateTodo(@PathVariable todoId: UUID, @Valid @RequestBody request: UpdateTodoRequest): TodoResponse {
        val todo = todos.findById(todoId).orElseThrow { notFound("Todo") }
        todo.isCompleted = request.isCompleted
        return todos.save(todo).toResponse()
    }

    private fun recording(id: UUID) = recordings.findById(id).orElseThrow { notFound("Recording") }
    private fun notFound(resource: String) = ResponseStatusException(HttpStatus.NOT_FOUND, "$resource not found")
    private fun validateTimePair(start: Long?, end: Long?) {
        if ((start == null) != (end == null) || (start != null && (start < 0 || end!! < start))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid source time range")
        }
    }
    private fun TodoEntity.toResponse() = TodoResponse(id, recordingId, summaryItemId, title, isCompleted, sourceStartMs, sourceEndMs, createdAt)
    private fun label(start: Long, end: Long) = "${format(start)}–${format(end)}"
    private fun format(value: Long) = "%02d:%02d".format(value / 60000, (value / 1000) % 60)
}

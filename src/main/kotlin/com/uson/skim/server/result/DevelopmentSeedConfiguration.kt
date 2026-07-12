package com.uson.skim.server.result

import com.uson.skim.server.recording.RecordingEntity
import com.uson.skim.server.recording.RecordingRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Configuration
class DevelopmentSeedConfiguration {
    @Bean
    fun seedDemoResult(
        recordings: RecordingRepository,
        chunks: TranscriptChunkRepository,
        summaries: SummaryItemRepository,
        sources: SummarySourceRepository,
    ) = CommandLineRunner {
        val recordingId = id("00000000-0000-0000-0000-000000000001")
        if (recordings.existsById(recordingId)) return@CommandLineRunner
        val createdAt = Instant.parse("2026-07-11T00:42:00Z")
        recordings.save(RecordingEntity(recordingId, "앱 아이디어 메모", 332000, "COMPLETED", null, createdAt, createdAt))

        val first = TranscriptChunkEntity(id("00000000-0000-0000-0000-000000000011"), recordingId, 105000, 150000,
            "핵심은 AI 요약 자체보다 원문 검증 경험이다. 요약 항목을 누르면 바로 근거 구간으로 이동해야 한다.", 0)
        val second = TranscriptChunkEntity(id("00000000-0000-0000-0000-000000000012"), recordingId, 160000, 190000,
            "요약 항목마다 timestamp를 붙이고, 사용자가 의심되는 항목을 탭하면 transcript chunk가 강조되어야 한다.", 1)
        chunks.saveAll(listOf(first, second))

        val summary = SummaryItemEntity(id("00000000-0000-0000-0000-000000000101"), recordingId, "차별점",
            "Skim은 AI 요약을 원문 timestamp와 연결해 사용자가 근거를 확인할 수 있게 한다.")
        summaries.save(summary)
        sources.saveAll(listOf(
            SummarySourceEntity(id("00000000-0000-0000-0000-000000000201"), summary.id, first.id, first.startMs, first.endMs),
            SummarySourceEntity(id("00000000-0000-0000-0000-000000000202"), summary.id, second.id, second.startMs, second.endMs),
        ))
    }

    private fun id(value: String): UUID = UUID.fromString(value)
}

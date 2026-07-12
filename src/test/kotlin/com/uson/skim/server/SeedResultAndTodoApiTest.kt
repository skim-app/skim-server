package com.uson.skim.server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class SeedResultAndTodoApiTest(@Autowired private val mockMvc: MockMvc) {
    private val seedId = "00000000-0000-0000-0000-000000000001"

    @Test
    fun `exposes deterministic completed result with source timestamp`() {
        mockMvc.get("/v1/recordings/$seedId/processing-status").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("COMPLETED") }
        }
        mockMvc.get("/v1/recordings/$seedId/transcript").andExpect {
            status { isOk() }
            jsonPath("$.chunks[0].startMs") { value(105000) }
            jsonPath("$.chunks[0].endMs") { value(150000) }
        }
        mockMvc.get("/v1/recordings/$seedId/summary").andExpect {
            status { isOk() }
            jsonPath("$.items[0].sources[0].transcriptChunkId") { exists() }
            jsonPath("$.items[0].sources[0].startMs") { value(105000) }
        }
    }

    @Test
    fun `creates and completes a todo linked to the seed summary`() {
        val created = mockMvc.post("/v1/todos") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
              "recordingId":"$seedId",
              "summaryItemId":"00000000-0000-0000-0000-000000000101",
              "title":"MVP 범위를 개인 음성 메모로 확정",
              "sourceStartMs":105000,
              "sourceEndMs":150000
            }"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.isCompleted") { value(false) }
        }.andReturn()
        val todoId = Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(created.response.contentAsString)!!.groupValues[1]

        mockMvc.patch("/v1/todos/$todoId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"isCompleted":true}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.isCompleted") { value(true) }
        }
    }
}

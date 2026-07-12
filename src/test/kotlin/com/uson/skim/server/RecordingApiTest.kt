package com.uson.skim.server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.mock.web.MockMultipartFile

@SpringBootTest
@AutoConfigureMockMvc
class RecordingApiTest(@Autowired private val mockMvc: MockMvc) {

    @Test
    fun `rejects an audio upload without multipart form data`() {
        mockMvc.post("/v1/recordings/00000000-0000-0000-0000-000000000001/audio").andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `uploads processes and streams an audio recording`() {
        val created = mockMvc.post("/v1/recordings") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"현장 메모"}"""
        }.andExpect { status { isCreated() } }.andReturn()
        val id = Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(created.response.contentAsString)!!.groupValues[1]
        val audio = MockMultipartFile("file", "memo.m4a", "audio/mp4", byteArrayOf(1, 2, 3, 4))

        mockMvc.multipart("/v1/recordings/$id/audio") { file(audio) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UPLOADED") }
        }
        mockMvc.post("/v1/recordings/$id/process").andExpect {
            status { isAccepted() }
        }
        mockMvc.get("/v1/recordings/$id/summary").andExpect {
            status { isOk() }
            jsonPath("$.items[0].sources[0].startMs") { value(0) }
        }
        mockMvc.get("/v1/recordings/$id/audio").andExpect {
            status { isOk() }
            header { string("Accept-Ranges", "bytes") }
            content { contentTypeCompatibleWith(MediaType("audio", "mp4")) }
        }
    }

    @Test
    fun `creates a recording and returns it from the list contract`() {
        val created = mockMvc.post("/v1/recordings") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"앱 아이디어 메모"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("앱 아이디어 메모") }
            jsonPath("$.status") { value("UPLOADED") }
            jsonPath("$.id") { exists() }
        }.andReturn()

        val id = Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(created.response.contentAsString)!!.groupValues[1]

        mockMvc.get("/v1/recordings") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(id) }
            jsonPath("$[0].title") { value("앱 아이디어 메모") }
        }
    }
}

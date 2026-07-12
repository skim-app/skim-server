CREATE TABLE recordings (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    duration_ms BIGINT,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT recording_duration_nonnegative CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE TABLE transcript_chunks (
    id UUID PRIMARY KEY,
    recording_id UUID NOT NULL REFERENCES recordings(id) ON DELETE CASCADE,
    start_ms BIGINT NOT NULL CHECK (start_ms >= 0),
    end_ms BIGINT NOT NULL CHECK (end_ms >= start_ms),
    text TEXT NOT NULL,
    sequence_number INTEGER NOT NULL CHECK (sequence_number >= 0),
    CONSTRAINT transcript_sequence_unique UNIQUE (recording_id, sequence_number)
);

CREATE TABLE summary_items (
    id UUID PRIMARY KEY,
    recording_id UUID NOT NULL REFERENCES recordings(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    text TEXT NOT NULL
);

CREATE TABLE summary_sources (
    id UUID PRIMARY KEY,
    summary_item_id UUID NOT NULL REFERENCES summary_items(id) ON DELETE CASCADE,
    transcript_chunk_id UUID NOT NULL REFERENCES transcript_chunks(id) ON DELETE RESTRICT,
    start_ms BIGINT NOT NULL CHECK (start_ms >= 0),
    end_ms BIGINT NOT NULL CHECK (end_ms >= start_ms)
);

CREATE TABLE todos (
    id UUID PRIMARY KEY,
    recording_id UUID NOT NULL REFERENCES recordings(id) ON DELETE CASCADE,
    summary_item_id UUID REFERENCES summary_items(id) ON DELETE SET NULL,
    title VARCHAR(300) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    source_start_ms BIGINT,
    source_end_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT todo_source_pair CHECK (
      (source_start_ms IS NULL AND source_end_ms IS NULL) OR
      (source_start_ms IS NOT NULL AND source_end_ms IS NOT NULL AND source_start_ms >= 0 AND source_end_ms >= source_start_ms)
    )
);

CREATE TABLE processing_jobs (
    id UUID PRIMARY KEY,
    recording_id UUID NOT NULL UNIQUE REFERENCES recordings(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transcript_chunks_recording ON transcript_chunks(recording_id, sequence_number);
CREATE INDEX idx_summary_items_recording ON summary_items(recording_id);
CREATE INDEX idx_todos_recording ON todos(recording_id);

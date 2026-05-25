CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    checksum VARCHAR(128) NOT NULL UNIQUE,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    chapter_code VARCHAR(50),
    chapter_title VARCHAR(255),
    indexed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE benchmark_results (
    id UUID PRIMARY KEY,
    experiment_type VARCHAR(20) NOT NULL,
    chunking_strategy VARCHAR(50) NOT NULL,
    embedding_model VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    ground_truth TEXT NOT NULL,
    generated_answer TEXT NOT NULL,
    exact_match DOUBLE PRECISION,
    f1_score DOUBLE PRECISION,
    faithfulness DOUBLE PRECISION,
    answer_relevancy DOUBLE PRECISION,
    context_precision DOUBLE PRECISION,
    context_recall DOUBLE PRECISION,
    latency_ms BIGINT,
    cost_usd NUMERIC(10,4),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    token_count INTEGER,
    chunking_strategy VARCHAR(50) NOT NULL,
    embedding_model VARCHAR(50) NOT NULL,
    vector_point_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    citation_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_course_chapter
    ON documents (course_code, chapter_code);

CREATE INDEX idx_chunks_document_chunk_index
    ON chunks (document_id, chunk_index);

CREATE INDEX idx_chunks_strategy_embedding
    ON chunks (chunking_strategy, embedding_model);

CREATE INDEX idx_messages_conversation_sequence
    ON messages (conversation_id, sequence_no);

CREATE INDEX idx_benchmark_results_experiment_strategy_embedding_created
    ON benchmark_results (experiment_type, chunking_strategy, embedding_model, created_at);

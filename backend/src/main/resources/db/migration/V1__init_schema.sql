-- //cập nhật thêm trường status của v2 
CREATE TABLE public.benchmark_results (
    benchmark_results_id UUID PRIMARY KEY,
    experiment_type      VARCHAR(50)    NOT NULL,
    chunking_strategy    VARCHAR(50)    NOT NULL,
    embedding_model      VARCHAR(50)    NOT NULL,
    question             TEXT           NOT NULL,
    ground_truth         TEXT           NOT NULL,
    generated_answer     TEXT           NOT NULL,
    exact_match          FLOAT8         NOT NULL,
    f1_score             FLOAT8         NOT NULL,
    faithfulness         FLOAT8         NOT NULL,
    answer_relevancy     FLOAT8         NOT NULL,
    context_precision    FLOAT8         NOT NULL,
    context_recall       FLOAT8         NOT NULL,
    latency_ms           INT8           NOT NULL,
    cost_usd             NUMERIC(10, 4) NOT NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE TABLE documents (
    documents_id  UUID          PRIMARY KEY,
    title         VARCHAR(255)  NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255)  NOT NULL,
    checksum      VARCHAR(64)   NOT NULL UNIQUE,
    course_code   VARCHAR(50),
    course_name   VARCHAR(150)  NOT NULL,
    chapter_code  VARCHAR(50),
    chapter_title VARCHAR(150),
    -- status được đưa vào ngay từ V1, không cần ALTER ở V2
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    indexed_at TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE TABLE chunks (
    chunks_id         UUID         PRIMARY KEY,
    documents_id      UUID         NOT NULL,
    chunk_index       INT4         NOT NULL,
    content           TEXT         NOT NULL,
    page_number       INT4,
    token_count       INT4         NOT NULL,
    chunking_strategy VARCHAR(50)  NOT NULL,
    embedding_model   VARCHAR(50)  NOT NULL,
    vector_point_id   VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
 
    CONSTRAINT fk_chunks_document
        FOREIGN KEY (documents_id) REFERENCES documents(documents_id) ON DELETE CASCADE
);
 
CREATE TABLE conversations (
    conversations_id UUID         PRIMARY KEY,
    session_id       VARCHAR(255) NOT NULL UNIQUE,
    title            VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE TABLE messages (
    messages_id      UUID    PRIMARY KEY,
    conversations_id UUID    NOT NULL,
    sequence_no      INT4    NOT NULL,
    role             VARCHAR(20) NOT NULL,
    content          TEXT    NOT NULL,
    citation_payload TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversations_id) REFERENCES conversations(conversations_id) ON DELETE CASCADE
);
 
CREATE TABLE citations (
    citations_id UUID    PRIMARY KEY,
    messages_id  UUID    NOT NULL,
    chunks_id    UUID    NOT NULL,
    chunk_id     UUID    NOT NULL,
    rank         INT4    NOT NULL,
    score        FLOAT8  NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 
    CONSTRAINT fk_citations_message
        FOREIGN KEY (messages_id) REFERENCES messages(messages_id) ON DELETE CASCADE,
    CONSTRAINT fk_citations_chunk
        FOREIGN KEY (chunks_id) REFERENCES chunks(chunks_id) ON DELETE CASCADE
);
 
CREATE INDEX idx_documents_course_chapter ON documents(course_code, chapter_code);
CREATE INDEX idx_chunks_document_index    ON chunks(documents_id, chunk_index);
CREATE INDEX idx_messages_conversation_seq ON messages(conversations_id, sequence_no);
CREATE INDEX idx_citations_message_id     ON citations(messages_id);
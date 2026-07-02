ALTER TABLE documents
ADD COLUMN IF NOT EXISTS conversation_session_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_documents_conversation_session_id
ON documents (conversation_session_id);

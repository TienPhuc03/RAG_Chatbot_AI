ALTER TABLE benchmark_results
    ALTER COLUMN chunking_strategy DROP NOT NULL;

ALTER TABLE benchmark_results
    ALTER COLUMN embedding_model DROP NOT NULL;

ALTER TABLE benchmark_results
    ADD COLUMN IF NOT EXISTS evaluation_source VARCHAR(50);

ALTER TABLE benchmark_results
    ADD COLUMN IF NOT EXISTS evaluation_fallback_used BOOLEAN NOT NULL DEFAULT FALSE;

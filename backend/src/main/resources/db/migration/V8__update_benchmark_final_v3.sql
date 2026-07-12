-- Thêm các ID và cấu hình truy vết
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS question_id VARCHAR(20);
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS run_id VARCHAR(100);
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS benchmark_mode VARCHAR(30);
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS config_key VARCHAR(200);

-- Thêm các cột Metric cho tầng Retrieval
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS hit_at_k DOUBLE PRECISION;
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS recall_at_k DOUBLE PRECISION;
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS reciprocal_rank DOUBLE PRECISION;
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS ndcg_at_k DOUBLE PRECISION;

-- Thêm Performance Metric và Tracking
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS embedding_latency_ms BIGINT;
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS search_latency_ms BIGINT;
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS item_status VARCHAR(20);
ALTER TABLE benchmark_results ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Đánh Index để tính năng Resume chạy nhanh
CREATE INDEX IF NOT EXISTS idx_benchmark_run_question_config
ON benchmark_results(run_id, question_id, config_key);
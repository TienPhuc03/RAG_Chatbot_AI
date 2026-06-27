    ALTER TABLE benchmark_results
        ADD COLUMN IF NOT EXISTS retrieval_hit BOOLEAN;
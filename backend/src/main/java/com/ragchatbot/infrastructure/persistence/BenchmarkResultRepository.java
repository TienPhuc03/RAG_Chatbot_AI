package com.ragchatbot.infrastructure.persistence;

import com.ragchatbot.domain.model.BenchmarkResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResult, UUID> {
}

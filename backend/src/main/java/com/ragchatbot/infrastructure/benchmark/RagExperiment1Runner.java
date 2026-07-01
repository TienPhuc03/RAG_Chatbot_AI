package com.ragchatbot.infrastructure.benchmark;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RagExperiment1Runner implements ApplicationRunner {

    private final BenchmarkRunnerService benchmarkRunnerService;
    private final BenchmarkJobRegistry jobRegistry;

    public RagExperiment1Runner(BenchmarkRunnerService benchmarkRunnerService,
                                 BenchmarkJobRegistry jobRegistry) {
        this.benchmarkRunnerService = benchmarkRunnerService;
        this.jobRegistry = jobRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Chỉ chạy khi có flag --benchmark=RAG_1
        if (!args.containsOption("benchmark") ||
            !args.getOptionValues("benchmark").contains("RAG_1")) {
            return;
        }

        System.out.println("=== Bắt đầu Benchmark RAG-1: 3 strategies × E5_BASE × Gemini ===");

        // Tổ hợp 1: FIXED_SIZE × MULTILINGUAL_E5_BASE × RAG_SYSTEM
        runWithJob(new BenchmarkRunnerService.BenchmarkConfig(
            "FIXED_SIZE", "MULTILINGUAL_E5_BASE", "RAG_SYSTEM", null
        ), "tổ hợp 1: FIXED_SIZE × E5_BASE");

        // Tổ hợp 2: SEMANTIC × MULTILINGUAL_E5_BASE × RAG_SYSTEM
        runWithJob(new BenchmarkRunnerService.BenchmarkConfig(
            "SEMANTIC", "MULTILINGUAL_E5_BASE", "RAG_SYSTEM", null
        ), "tổ hợp 2: SEMANTIC × E5_BASE");

        // Tổ hợp 3: HIERARCHICAL × MULTILINGUAL_E5_BASE × RAG_SYSTEM
        runWithJob(new BenchmarkRunnerService.BenchmarkConfig(
            "HIERARCHICAL", "MULTILINGUAL_E5_BASE", "RAG_SYSTEM", null
        ), "tổ hợp 3: HIERARCHICAL × E5_BASE");

        System.out.println("=== Benchmark RAG-1 hoàn thành ===");
    }

    private void runWithJob(BenchmarkRunnerService.BenchmarkConfig config, String label) {
        String jobId = UUID.randomUUID().toString();
        jobRegistry.register(jobId, label, 0);
        benchmarkRunnerService.runBenchmark(jobId, config);
        System.out.println(">>> Hoàn thành " + label);
    }
}
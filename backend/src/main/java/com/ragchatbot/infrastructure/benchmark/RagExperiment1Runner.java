package com.ragchatbot.infrastructure.benchmark;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RagExperiment1Runner implements ApplicationRunner {

    private final BenchmarkRunnerService benchmarkRunnerService;

    public RagExperiment1Runner(BenchmarkRunnerService benchmarkRunnerService) {
        this.benchmarkRunnerService = benchmarkRunnerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Chỉ chạy khi có flag --benchmark=RAG_1
        if (!args.containsOption("benchmark") ||
            !args.getOptionValues("benchmark").contains("RAG_1")) {
            return;
        }

        System.out.println("=== Bắt đầu Benchmark RAG-1: 3 strategies × E5_BASE × Gemini ===");

        // Tổ hợp 1: FIXED_SIZE × MULTILINGUAL_E5_BASE × RAG
        benchmarkRunnerService.runBenchmark(new BenchmarkRunnerService.BenchmarkConfig(
            "FIXED_SIZE",
            "MULTILINGUAL_E5_BASE",
            "RAG"
        ));
        System.out.println(">>> Hoàn thành tổ hợp 1: FIXED_SIZE × E5_BASE");

        // Tổ hợp 2: SEMANTIC × MULTILINGUAL_E5_BASE × RAG
        benchmarkRunnerService.runBenchmark(new BenchmarkRunnerService.BenchmarkConfig(
            "SEMANTIC",
            "MULTILINGUAL_E5_BASE",
            "RAG"
        ));
        System.out.println(">>> Hoàn thành tổ hợp 2: SEMANTIC × E5_BASE");

        // Tổ hợp 3: HIERARCHICAL × MULTILINGUAL_E5_BASE × RAG
        benchmarkRunnerService.runBenchmark(new BenchmarkRunnerService.BenchmarkConfig(
            "HIERARCHICAL",
            "MULTILINGUAL_E5_BASE",
            "RAG"
        ));
        System.out.println(">>> Hoàn thành tổ hợp 3: HIERARCHICAL × E5_BASE");

        System.out.println("=== Benchmark RAG-1 hoàn thành ===");
    }
}
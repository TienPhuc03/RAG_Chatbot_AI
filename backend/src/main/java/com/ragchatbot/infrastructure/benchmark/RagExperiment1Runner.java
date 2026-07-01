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
        if (!args.containsOption("benchmark") || !args.getOptionValues("benchmark").contains("RAG_1")) {
            return;
        }

        benchmarkRunnerService.runBenchmark(
                "rag-1-fixed-size",
                new BenchmarkRunnerService.BenchmarkConfig("FIXED_SIZE", "MULTILINGUAL_E5_BASE", "RAG")
        );
        benchmarkRunnerService.runBenchmark(
                "rag-1-semantic",
                new BenchmarkRunnerService.BenchmarkConfig("SEMANTIC", "MULTILINGUAL_E5_BASE", "RAG")
        );
        benchmarkRunnerService.runBenchmark(
                "rag-1-hierarchical",
                new BenchmarkRunnerService.BenchmarkConfig("HIERARCHICAL", "MULTILINGUAL_E5_BASE", "RAG")
        );
    }
}

package com.ragchatbot.domain.model;

import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.ExperimentType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
// import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "benchmark_results")
public class BenchmarkResult {

    @Id
    @Column(name = "benchmark_results_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "experiment_type", nullable = false, length = 20)
    private ExperimentType experimentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunking_strategy", nullable = false, length = 50)
    private ChunkingStrategy chunkingStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_model", nullable = false, length = 50)
    private EmbeddingModel embeddingModel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "ground_truth", nullable = false, columnDefinition = "TEXT")
    private String groundTruth;

    @Column(name = "generated_answer", nullable = false, columnDefinition = "TEXT")
    private String generatedAnswer;

    @Column(name = "exact_match")
    private Double exactMatch;

    @Column(name = "f1_score")
    private Double f1Score;

    @Column
    private Double faithfulness;

    @Column(name = "answer_relevancy")
    private Double answerRelevancy;

    @Column(name = "context_precision")
    private Double contextPrecision;

    @Column(name = "context_recall")
    private Double contextRecall;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ExperimentType getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(ExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(ChunkingStrategy chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getGroundTruth() {
        return groundTruth;
    }

    public void setGroundTruth(String groundTruth) {
        this.groundTruth = groundTruth;
    }

    public String getGeneratedAnswer() {
        return generatedAnswer;
    }

    public void setGeneratedAnswer(String generatedAnswer) {
        this.generatedAnswer = generatedAnswer;
    }

    public Double getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Double exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Double getF1Score() {
        return f1Score;
    }

    public void setF1Score(Double f1Score) {
        this.f1Score = f1Score;
    }

    public Double getFaithfulness() {
        return faithfulness;
    }

    public void setFaithfulness(Double faithfulness) {
        this.faithfulness = faithfulness;
    }

    public Double getAnswerRelevancy() {
        return answerRelevancy;
    }

    public void setAnswerRelevancy(Double answerRelevancy) {
        this.answerRelevancy = answerRelevancy;
    }

    public Double getContextPrecision() {
        return contextPrecision;
    }

    public void setContextPrecision(Double contextPrecision) {
        this.contextPrecision = contextPrecision;
    }

    public Double getContextRecall() {
        return contextRecall;
    }

    public void setContextRecall(Double contextRecall) {
        this.contextRecall = contextRecall;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.ragchatbot.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "benchmark_results")
public class BenchmarkResult {

    @Id
    @Column(name = "benchmark_results_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- CÁC TRƯỜNG ĐÃ BỔ SUNG THEO CHUẨN FINAL V3.0 ---
    @Column(name = "question_id", length = 20)
    private String questionId;

    @Column(name = "run_id", length = 100)
    private String runId;

    @Column(name = "benchmark_mode", length = 30)
    private String benchmarkMode;

    @Column(name = "config_key", length = 200)
    private String configKey;

    @Column(name = "hit_at_k")
    private Double hitAtK;

    @Column(name = "recall_at_k")
    private Double recallAtK;

    @Column(name = "reciprocal_rank")
    private Double reciprocalRank;

    @Column(name = "ndcg_at_k")
    private Double ndcgAtK;

    @Column(name = "embedding_latency_ms")
    private Long embeddingLatencyMs;

    @Column(name = "search_latency_ms")
    private Long searchLatencyMs;

    @Column(name = "item_status", length = 20)
    private String itemStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    // -----------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "experiment_type", nullable = false, length = 20)
    private ExperimentType experimentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunking_strategy", length = 50)
    private ChunkingStrategy chunkingStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_model", length = 50)
    private EmbeddingModel embeddingModel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "ground_truth", nullable = false, columnDefinition = "TEXT")
    private String groundTruth;

    @Column(name = "generated_answer", columnDefinition = "TEXT")
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

    @Column(name = "retrieval_hit")
    private Boolean retrievalHit;

    @Column(name = "evaluation_source", length = 50)
    private String evaluationSource;

    @Column(name = "evaluation_fallback_used", nullable = false)
    private Boolean evaluationFallbackUsed = Boolean.FALSE;

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

    // --- GETTERS & SETTERS ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getBenchmarkMode() { return benchmarkMode; }
    public void setBenchmarkMode(String benchmarkMode) { this.benchmarkMode = benchmarkMode; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public Double getHitAtK() { return hitAtK; }
    public void setHitAtK(Double hitAtK) { this.hitAtK = hitAtK; }

    public Double getRecallAtK() { return recallAtK; }
    public void setRecallAtK(Double recallAtK) { this.recallAtK = recallAtK; }

    public Double getReciprocalRank() { return reciprocalRank; }
    public void setReciprocalRank(Double reciprocalRank) { this.reciprocalRank = reciprocalRank; }

    public Double getNdcgAtK() { return ndcgAtK; }
    public void setNdcgAtK(Double ndcgAtK) { this.ndcgAtK = ndcgAtK; }

    public Long getEmbeddingLatencyMs() { return embeddingLatencyMs; }
    public void setEmbeddingLatencyMs(Long embeddingLatencyMs) { this.embeddingLatencyMs = embeddingLatencyMs; }

    public Long getSearchLatencyMs() { return searchLatencyMs; }
    public void setSearchLatencyMs(Long searchLatencyMs) { this.searchLatencyMs = searchLatencyMs; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public ExperimentType getExperimentType() { return experimentType; }
    public void setExperimentType(ExperimentType experimentType) { this.experimentType = experimentType; }

    public ChunkingStrategy getChunkingStrategy() { return chunkingStrategy; }
    public void setChunkingStrategy(ChunkingStrategy chunkingStrategy) { this.chunkingStrategy = chunkingStrategy; }

    public EmbeddingModel getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(EmbeddingModel embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getGroundTruth() { return groundTruth; }
    public void setGroundTruth(String groundTruth) { this.groundTruth = groundTruth; }

    public String getGeneratedAnswer() { return generatedAnswer; }
    public void setGeneratedAnswer(String generatedAnswer) { this.generatedAnswer = generatedAnswer; }

    public Double getExactMatch() { return exactMatch; }
    public void setExactMatch(Double exactMatch) { this.exactMatch = exactMatch; }

    public Double getF1Score() { return f1Score; }
    public void setF1Score(Double f1Score) { this.f1Score = f1Score; }

    public Double getFaithfulness() { return faithfulness; }
    public void setFaithfulness(Double faithfulness) { this.faithfulness = faithfulness; }

    public Double getAnswerRelevancy() { return answerRelevancy; }
    public void setAnswerRelevancy(Double answerRelevancy) { this.answerRelevancy = answerRelevancy; }

    public Double getContextPrecision() { return contextPrecision; }
    public void setContextPrecision(Double contextPrecision) { this.contextPrecision = contextPrecision; }

    public Double getContextRecall() { return contextRecall; }
    public void setContextRecall(Double contextRecall) { this.contextRecall = contextRecall; }

    public Boolean getRetrievalHit() { return retrievalHit; }
    public void setRetrievalHit(Boolean retrievalHit) { this.retrievalHit = retrievalHit; }

    public String getEvaluationSource() { return evaluationSource; }
    public void setEvaluationSource(String evaluationSource) { this.evaluationSource = evaluationSource; }

    public Boolean getEvaluationFallbackUsed() { return evaluationFallbackUsed; }
    public void setEvaluationFallbackUsed(Boolean evaluationFallbackUsed) { this.evaluationFallbackUsed = evaluationFallbackUsed; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

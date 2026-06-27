package com.ragchatbot.domain.model;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
// import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "chunks")
public class Chunk {

    @Id
    @Column(name = "chunks_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documents_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunking_strategy", nullable = false, length = 50)
    private ChunkingStrategy chunkingStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_model", nullable = false, length = 50)
    private EmbeddingModel embeddingModel;

    @Column(name = "vector_point_id", length = 128)
    private String vectorPointId;

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

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(ChunkingStrategy fixedSize) {
        this.chunkingStrategy = fixedSize;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getVectorPointId() {
        return vectorPointId;
    }

    public void setVectorPointId(String vectorPointId) {
        this.vectorPointId = vectorPointId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

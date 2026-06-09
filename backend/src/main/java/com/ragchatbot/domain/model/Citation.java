package com.ragchatbot.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "citations")

public class Citation {
    @Id
    @Column(name = "citations_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messages_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunks_id", nullable = false)
    private Chunk chunk;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Double score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }   
    public void setId(UUID id) {
        this.id = id;
    }
    public Message getMessage() {
        return message;
    }
    public void setMessage(Message message) {
        this.message = message;
    }
    public Chunk getChunk() {
        return chunk;
    }
    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }
    public UUID getChunkId() {
        return chunkId;
    }
    public void setChunkId(UUID chunkId) {
        this.chunkId = chunkId;
    }
    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    public Double getScore() {
        return score;
    }
    public void setScore(Double score) {
        this.score = score;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}

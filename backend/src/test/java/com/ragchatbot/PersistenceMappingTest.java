package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.ExperimentType;
import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.model.BenchmarkResult;
import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.model.Message;
import com.ragchatbot.domain.model.Citation;
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import com.ragchatbot.infrastructure.persistence.CitationRepository; // Đảm bảo bạn đã tạo interface này
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

// @Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceMappingTest extends PostgresIntegrationTestSupport {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private BenchmarkResultRepository benchmarkResultRepository;

    @Autowired
    private CitationRepository citationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesDocumentAndChunk() {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Bai giang Chuong 1");
        document.setSourceFileName("chuong-1.pdf");
        document.setContentType("application/pdf");
        document.setChecksum("doc-checksum-001");
        document.setCourseCode("JAVA101");
        document.setCourseName("Lap trinh Java");
        document.setChapterCode("CH1");
        document.setChapterTitle("Tong quan");
        
        Document savedDocument = documentRepository.saveAndFlush(document);

        Chunk chunk = new Chunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(savedDocument);
        chunk.setChunkIndex(0);
        chunk.setContent("Noi dung duoc chia nho.");
        chunk.setPageNumber(1);
        chunk.setTokenCount(42);
        chunk.setChunkingStrategy(ChunkingStrategy.FIXED_SIZE);
        chunk.setEmbeddingModel(EmbeddingModel.MULTILINGUAL_E5_BASE);
        chunk.setVectorPointId("point-001");

        Chunk savedChunk = chunkRepository.saveAndFlush(chunk);

        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getDocument().getId()).isEqualTo(savedDocument.getId());
    }

    @Test
    void savesConversationAndMessage() {
        Conversation conversation = new Conversation();
        conversation.setSessionId("session-001");
        conversation.setTitle("Hoi dap Chuong 1");

        Conversation savedConversation = conversationRepository.saveAndFlush(conversation);

        Message message = new Message();
        message.setConversation(savedConversation);
        message.setSequenceNo(1);
        message.setRole(MessageRole.USER);
        message.setContent("Java la gi?");
        message.setCitationPayload("{\"sources\":[]}");

        Message savedMessage = messageRepository.saveAndFlush(message);

        assertThat(conversationRepository.findBySessionId("session-001")).isPresent();
        assertThat(savedMessage.getConversation().getId()).isEqualTo(savedConversation.getId());
    }

    @Test
    void savesBenchmarkResult() {
        BenchmarkResult benchmarkResult = new BenchmarkResult();
        // benchmarkResult.setId(UUID.randomUUID());
        benchmarkResult.setExperimentType(ExperimentType.RAG);
        benchmarkResult.setChunkingStrategy(ChunkingStrategy.SEMANTIC);
        benchmarkResult.setEmbeddingModel(EmbeddingModel.BGE_M3);
        benchmarkResult.setQuestion("Java dung de lam gi?");
        benchmarkResult.setGroundTruth("Java duoc dung de phat trien phan mem.");
        benchmarkResult.setGeneratedAnswer("Java duoc dung de phat trien phan mem.");
        benchmarkResult.setExactMatch(1.0);
        benchmarkResult.setF1Score(1.0);
        benchmarkResult.setFaithfulness(0.9);
        benchmarkResult.setAnswerRelevancy(0.88);
        benchmarkResult.setContextPrecision(0.86);
        benchmarkResult.setContextRecall(0.84);
        benchmarkResult.setEvaluationSource("ragas-service:gemini");
        benchmarkResult.setEvaluationFallbackUsed(false);
        benchmarkResult.setLatencyMs(1234L);
        benchmarkResult.setCostUsd(new BigDecimal("0.0123"));

        BenchmarkResult savedResult = benchmarkResultRepository.saveAndFlush(benchmarkResult);

        assertThat(savedResult.getId()).isNotNull();
        assertThat(benchmarkResultRepository.count()).isEqualTo(1);
    }

    @Test
    void aggregatesBenchmarkSummaryIncludingFallbackAndRetrievalRate() {
        BenchmarkResult first = new BenchmarkResult();
        first.setExperimentType(ExperimentType.RAG);
        first.setChunkingStrategy(ChunkingStrategy.SEMANTIC);
        first.setEmbeddingModel(EmbeddingModel.BGE_M3);
        first.setQuestion("Q1");
        first.setGroundTruth("A1");
        first.setGeneratedAnswer("A1");
        first.setExactMatch(1.0);
        first.setF1Score(1.0);
        first.setFaithfulness(0.9);
        first.setAnswerRelevancy(0.8);
        first.setContextPrecision(0.7);
        first.setContextRecall(0.6);
        first.setRetrievalHit(true);
        first.setEvaluationSource("ragas-service:gemini");
        first.setEvaluationFallbackUsed(false);
        first.setLatencyMs(100L);
        first.setCostUsd(new BigDecimal("0.0100"));

        BenchmarkResult second = new BenchmarkResult();
        second.setExperimentType(ExperimentType.RAG);
        second.setChunkingStrategy(ChunkingStrategy.SEMANTIC);
        second.setEmbeddingModel(EmbeddingModel.BGE_M3);
        second.setQuestion("Q2");
        second.setGroundTruth("A2");
        second.setGeneratedAnswer("A2");
        second.setExactMatch(0.0);
        second.setF1Score(0.5);
        second.setFaithfulness(0.4);
        second.setAnswerRelevancy(0.5);
        second.setContextPrecision(0.6);
        second.setContextRecall(0.7);
        second.setRetrievalHit(false);
        second.setEvaluationSource("local-fallback");
        second.setEvaluationFallbackUsed(true);
        second.setLatencyMs(300L);
        second.setCostUsd(new BigDecimal("0.0300"));

        benchmarkResultRepository.saveAllAndFlush(List.of(first, second));

        var summaries = benchmarkResultRepository.findAverageMetricsByStrategyAndModel();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().chunkingStrategy()).isEqualTo(ChunkingStrategy.SEMANTIC);
        assertThat(summaries.getFirst().embeddingModel()).isEqualTo(EmbeddingModel.BGE_M3);  
        assertThat(summaries.getFirst().runCount()).isEqualTo(2);
        assertThat(summaries.getFirst().fallbackRunCount()).isEqualTo(1);
        assertThat(summaries.getFirst().ragasRunCount()).isEqualTo(1);
        assertThat(summaries.getFirst().geminiJudgeRunCount()).isEqualTo(1);
        assertThat(summaries.getFirst().ollamaJudgeRunCount()).isEqualTo(0);
        assertThat(summaries.getFirst().retrievalHitRate()).isEqualTo(0.5);
    }

    @Test
    void savesCitation() {
        // 1. Tạo Document mẫu
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Tai lieu RAG");
        document.setSourceFileName("rag.pdf");
        document.setContentType("application/pdf");
        document.setChecksum("checksum-111");
        document.setCourseCode("JAVA101");
        document.setCourseName("Lap trinh Java");
        Document savedDoc = documentRepository.saveAndFlush(document);

        // 2. Tạo Chunk mẫu
        Chunk chunk = new Chunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(savedDoc);
        chunk.setChunkIndex(1);
        chunk.setContent("Doan van ban mau dung de trich dan.");
        chunk.setTokenCount(20);
        chunk.setChunkingStrategy(ChunkingStrategy.FIXED_SIZE);
        chunk.setEmbeddingModel(EmbeddingModel.BGE_M3);
        chunk.setVectorPointId("vector-111");
        Chunk savedChunk = chunkRepository.saveAndFlush(chunk);

        // 3. Tạo Conversation mẫu
        Conversation conversation = new Conversation();
        conversation.setSessionId("session-999");
        Conversation savedConv = conversationRepository.saveAndFlush(conversation);

        // 4. Tạo Message mẫu
        Message message = new Message();
        message.setConversation(savedConv);
        message.setSequenceNo(1);
        message.setRole(MessageRole.ASSISTANT);
        message.setContent("Cau tra loi co trich dan.");
        Message savedMessage = messageRepository.saveAndFlush(message);

        // 5. Tạo và Lưu Citation để test mapping liên kết n-n theo ERD
        Citation citation = new Citation();
        citation.setId(UUID.randomUUID());
        citation.setMessage(savedMessage);
        citation.setChunk(savedChunk);
        citation.setChunkId(savedChunk.getId()); // Trường thông tin phụ uuid chunk_id
        citation.setRank(1);
        citation.setScore(0.95);

        Citation savedCitation = citationRepository.saveAndFlush(citation);

        // Đối soát kết quả lưu dữ liệu trích dẫn
        assertThat(savedCitation.getId()).isNotNull();
        assertThat(savedCitation.getMessage().getId()).isEqualTo(savedMessage.getId());
        assertThat(savedCitation.getChunk().getId()).isEqualTo(savedChunk.getId());
    }

    @Test
    void createsExpectedIndexes() {
        List<String> indexNames = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'",
                String.class
        );

        assertThat(indexNames).contains(
                "documents_pkey",
                "conversations_pkey",
                "benchmark_results_pkey",
                "chunks_pkey",
                "messages_pkey",
                "citations_pkey", // Kiểm tra xem bảng trích dẫn đã có khóa chính chưa
                "conversations_session_id_key",
                "idx_documents_course_chapter",
                "idx_chunks_document_index",
                "idx_messages_conversation_seq",
                "idx_citations_message_id" // Đối soát index tìm kiếm của bảng citations
        );
    }
}

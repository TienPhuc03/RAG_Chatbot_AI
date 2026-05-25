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
import com.ragchatbot.infrastructure.persistence.BenchmarkResultRepository;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
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
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesDocumentAndChunk() {
        Document document = new Document();
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
        benchmarkResult.setLatencyMs(1234L);
        benchmarkResult.setCostUsd(new BigDecimal("0.0123"));

        BenchmarkResult savedResult = benchmarkResultRepository.saveAndFlush(benchmarkResult);

        assertThat(savedResult.getId()).isNotNull();
        assertThat(benchmarkResultRepository.count()).isEqualTo(1);
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
                "conversations_session_id_key",
                "documents_checksum_key",
                "idx_documents_course_chapter",
                "idx_chunks_document_chunk_index",
                "idx_chunks_strategy_embedding",
                "idx_messages_conversation_sequence",
                "idx_benchmark_results_experiment_strategy_embedding_created"
        );
    }
}

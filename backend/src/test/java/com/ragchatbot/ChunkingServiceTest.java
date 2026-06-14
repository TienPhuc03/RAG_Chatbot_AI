package com.ragchatbot;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.infrastructure.chunking.FixedSizeChunkingService;
import com.ragchatbot.infrastructure.chunking.HierarchicalChunkingService;
import com.ragchatbot.infrastructure.chunking.SemanticChunkingService;

class ChunkingServiceTest {
    private final FixedSizeChunkingService fixedSizeService = new FixedSizeChunkingService();
    private final SemanticChunkingService semanticService = new SemanticChunkingService();
    private final HierarchicalChunkingService hierarchicalService = new HierarchicalChunkingService();

    // Input dùng chung cho cả 3 strategies
    private static final String SHARED_INPUT =
        "Học lập trình Java rất thú vị. Spring Boot giúp xây dựng API nhanh.\n\n" +
        "Các bước học:\n" +
        "- Học cú pháp cơ bản\n" +
        "- Học OOP\n" +
        "- Học Spring Framework";

    private static final ChunkingOptions OPTIONS = new ChunkingOptions(512, 50);

    // ------------------------------------------------------------------ //
    // Test 1: FixedSize — không mất text (total coverage)
    // ------------------------------------------------------------------ //
    @Test
    void testFixedSize_totalCoverage() {
        List<ChunkDraft> chunks = fixedSizeService.chunk(
            SHARED_INPUT, ChunkingStrategy.FIXED_SIZE, OPTIONS
        );

        assertThat(chunks).isNotEmpty();

        // Ghép lại (bỏ overlap) phải khớp với input gốc
        StringBuilder rebuilt = new StringBuilder();
        int overlap = OPTIONS.chunkOverlap();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i).content();
            rebuilt.append(i == 0 ? content : content.substring(Math.min(overlap, content.length())));
        }
        assertThat(rebuilt.toString()).isEqualTo(SHARED_INPUT);
    }

    // ------------------------------------------------------------------ //
    // Test 2: Semantic — không mất text (total coverage)
    // ------------------------------------------------------------------ //
    @Test
    void testSemantic_totalCoverage() {
        List<ChunkDraft> chunks = semanticService.chunk(
            SHARED_INPUT, ChunkingStrategy.SEMANTIC, OPTIONS
        );

        assertThat(chunks).isNotEmpty();

        // Ghép toàn bộ nội dung các chunk lại
        String combined = chunks.stream()
            .map(ChunkDraft::content)
            .reduce("", (a, b) -> a + " " + b)
            .strip();

        // Các từ khóa quan trọng không bị mất
        assertThat(combined).contains("lập trình Java");
        assertThat(combined).contains("Spring Boot");
        assertThat(combined).contains("Spring Framework");
    }

    // ------------------------------------------------------------------ //
    // Test 3: Hierarchical — không mất text (total coverage)
    // ------------------------------------------------------------------ //
    @Test
    void testHierarchical_totalCoverage() {
        List<ChunkDraft> chunks = hierarchicalService.chunk(
            SHARED_INPUT, ChunkingStrategy.HIERARCHICAL, OPTIONS
        );

        assertThat(chunks).isNotEmpty();

        // Tất cả nội dung gộp lại phải chứa đủ các từ khóa
        String combined = chunks.stream()
            .map(ChunkDraft::content)
            .reduce("", (a, b) -> a + " " + b)
            .strip();

        assertThat(combined).contains("lập trình Java");
        assertThat(combined).contains("Spring Boot");
        assertThat(combined).contains("Spring Framework");
    }

    // ------------------------------------------------------------------ //
    // Test 4: Hierarchical — parentChunkId đúng
    // ------------------------------------------------------------------ //
    @Test
    void testHierarchical_parentChunkId() {
        List<ChunkDraft> chunks = hierarchicalService.chunk(
            SHARED_INPUT, ChunkingStrategy.HIERARCHICAL, OPTIONS
        );

        assertThat(chunks).isNotEmpty();

        // Chunk cha (level 1) phải có parentChunkId = null
        List<ChunkDraft> parentChunks = chunks.stream()
            .filter(c -> c.parentChunkId() == null)
            .toList();
        assertThat(parentChunks).isNotEmpty();

        // Chunk con (level 2) phải có parentChunkId >= 0
        List<ChunkDraft> childChunks = chunks.stream()
            .filter(c -> c.parentChunkId() != null)
            .toList();

        for (ChunkDraft child : childChunks) {
            // parentChunkId phải trỏ về 1 chunk cha thực sự tồn tại
            assertThat(child.parentChunkId()).isGreaterThanOrEqualTo(0);

            boolean parentExists = chunks.stream()
                .anyMatch(p -> p.chunkIndex() == child.parentChunkId() && p.parentChunkId() == null);
            assertThat(parentExists)
                .as("parentChunkId=%d phải trỏ về chunk cha hợp lệ", child.parentChunkId())
                .isTrue();
        }
    }

    // ------------------------------------------------------------------ //
    // Test 5: Hierarchical — section có bullet sinh ra chunk con
    // ------------------------------------------------------------------ //
    @Test
    void testHierarchical_bulletSectionHasChildren() {
        List<ChunkDraft> chunks = hierarchicalService.chunk(
            SHARED_INPUT, ChunkingStrategy.HIERARCHICAL, OPTIONS
        );

        // Phải có ít nhất 1 chunk con chứa nội dung bullet
        boolean hasBulletChild = chunks.stream()
            .filter(c -> c.parentChunkId() != null)
            .anyMatch(c -> c.content().startsWith("-") || c.content().startsWith("*"));

        assertThat(hasBulletChild).isTrue();
    }

    // ------------------------------------------------------------------ //
    // Test 6: Cả 3 strategies — chunkIndex liên tục, không nhảy cóc
    // ------------------------------------------------------------------ //
    @Test
    void testAllStrategies_chunkIndexIsSequential() {
        List<ChunkDraft> fixed = fixedSizeService.chunk(
            SHARED_INPUT, ChunkingStrategy.FIXED_SIZE, OPTIONS
        );
        List<ChunkDraft> semantic = semanticService.chunk(
            SHARED_INPUT, ChunkingStrategy.SEMANTIC, OPTIONS
        );
        List<ChunkDraft> hierarchical = hierarchicalService.chunk(
            SHARED_INPUT, ChunkingStrategy.HIERARCHICAL, OPTIONS
        );

        for (int i = 0; i < fixed.size(); i++) {
            assertThat(fixed.get(i).chunkIndex()).isEqualTo(i);
        }
        for (int i = 0; i < semantic.size(); i++) {
            assertThat(semantic.get(i).chunkIndex()).isEqualTo(i);
        }
        for (int i = 0; i < hierarchical.size(); i++) {
            assertThat(hierarchical.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    // ------------------------------------------------------------------ //
    // Test 7: Input null/rỗng — cả 3 strategies không ném exception
    // ------------------------------------------------------------------ //
    @Test
    void testAllStrategies_nullAndEmptyInput() {
        assertThat(fixedSizeService.chunk(null, ChunkingStrategy.FIXED_SIZE, OPTIONS)).isEmpty();
        assertThat(fixedSizeService.chunk("", ChunkingStrategy.FIXED_SIZE, OPTIONS)).isEmpty();

        assertThat(semanticService.chunk(null, ChunkingStrategy.SEMANTIC, OPTIONS)).isEmpty();
        assertThat(semanticService.chunk("", ChunkingStrategy.SEMANTIC, OPTIONS)).isEmpty();

        assertThat(hierarchicalService.chunk(null, ChunkingStrategy.HIERARCHICAL, OPTIONS)).isEmpty();
        assertThat(hierarchicalService.chunk("", ChunkingStrategy.HIERARCHICAL, OPTIONS)).isEmpty();
    }
}




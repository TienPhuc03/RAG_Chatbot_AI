package com.ragchatbot.infrastructure.chunking;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;

@Component
public class ChunkingServiceFactory {
    
    private final Map<ChunkingStrategy, ChunkingService> serviceMap;

    public ChunkingServiceFactory(List<ChunkingService> chunkingServices) {
        this.serviceMap = chunkingServices.stream()
            .collect(Collectors.toMap(
                service -> resolveStrategy(service),
                Function.identity()
            ));
    }

    /**
     * Trả về ChunkingService phù hợp với strategy yêu cầu.
     *
     * @param strategy chiến lược chunking cần dùng
     * @return ChunkingService tương ứng
     * @throws IllegalArgumentException nếu không tìm thấy implementation cho strategy đó
     */
    public ChunkingService getService(ChunkingStrategy strategy) {
        ChunkingService service = serviceMap.get(strategy);
        if (service == null) {
            throw new IllegalArgumentException(
                "Không tìm thấy ChunkingService cho strategy: " + strategy
            );
        }
        return service;
    }

    /**
     * Delegate trực tiếp: lấy service theo strategy rồi gọi chunk() luôn.
     * Tiện cho caller không cần gọi getService() riêng.
     */
    public List<ChunkDraft> chunk(String rawText, ChunkingStrategy strategy, ChunkingOptions options) {
        return getService(strategy).chunk(rawText, strategy, options);
    }

    /**
     * Xác định ChunkingStrategy của một service dựa trên kiểu class.
     * Mỗi implementation ánh xạ 1-1 với 1 strategy trong enum.
     */
    private ChunkingStrategy resolveStrategy(ChunkingService service) {
        if (service instanceof FixedSizeChunkingService) {
            return ChunkingStrategy.FIXED_SIZE;
        }
        if (service instanceof SemanticChunkingService) {
            return ChunkingStrategy.SEMANTIC;
        }
        if (service instanceof HierarchicalChunkingService){
            return ChunkingStrategy.HIERARCHICAL;
        }
        throw new IllegalArgumentException(
            "Không thể xác định ChunkingStrategy cho service: " + service.getClass().getSimpleName()
        );
    }
}



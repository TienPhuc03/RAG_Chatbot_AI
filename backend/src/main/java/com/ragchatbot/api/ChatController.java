package com.ragchatbot.api;

import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.application.usecase.chat.GetChatHistoryUseCase;
import com.ragchatbot.application.usecase.chat.SendMessageUseCase;
import com.ragchatbot.domain.model.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;

    public ChatController(
            SendMessageUseCase sendMessageUseCase,
            GetChatHistoryUseCase getChatHistoryUseCase
    ) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.getChatHistoryUseCase = getChatHistoryUseCase;
    }

    /**
     * Gửi câu hỏi tới hệ thống RAG.
     */
    @PostMapping("/message")
    public ChatResponse sendMessage(
            @RequestBody ChatRequest request
    ) {
        return sendMessageUseCase.execute(request);
    }

    /**
     * Lấy lịch sử hội thoại.
     */
    @GetMapping("/history/{sessionId}")
    public List<Message> getChatHistory(
            @PathVariable String sessionId
    ) {
        return getChatHistoryUseCase.execute(sessionId);
    }
}
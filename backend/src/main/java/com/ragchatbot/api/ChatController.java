package com.ragchatbot.api;

import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.application.usecase.chat.GetChatHistoryUseCase;
import com.ragchatbot.application.usecase.chat.SendMessageUseCase;
import com.ragchatbot.domain.model.Message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


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
     * API gửi câu hỏi tới hệ thống RAG Chatbot.
     *
     * Flow:
     * User -> Controller -> SendMessageUseCase
     *      -> Retrieve documents
     *      -> LLM generate answer
     *      -> Return ChatResponse
     */
    @Operation(
            summary = "Send message to chatbot",
            description = "Gửi câu hỏi tới hệ thống RAG Chatbot và nhận câu trả lời."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Chat successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            )
    })
    @PostMapping("/message")
    public ChatResponse sendMessage(
            @Valid
            @RequestBody ChatRequest request
    ) {
        return sendMessageUseCase.execute(request);
    }

    /**
     * API lấy lịch sử hội thoại.
     *
     * Trả về danh sách message theo sessionId.
     */
    @Operation(
            summary = "Get chat history",
            description = "Lấy toàn bộ lịch sử hội thoại theo sessionId."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Get history successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found"
            )
    })
    @GetMapping("/history/{sessionId}")
    public List<Message> getChatHistory(

            @Parameter(
                    description = "Session ID của cuộc hội thoại",
                    example = "session-001"
            )
            @PathVariable
            String sessionId

    ) {
        return getChatHistoryUseCase.execute(sessionId);
    }
}
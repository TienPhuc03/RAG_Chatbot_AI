package com.ragchatbot.api;

import com.ragchatbot.application.dto.chat.ChatAttachmentItemResponse;
import com.ragchatbot.application.dto.chat.ChatAttachmentUploadResponse;
import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.application.dto.chat.ChatHistoryMessageDto;
import com.ragchatbot.application.dto.chat.ConversationSummaryDto;
import com.ragchatbot.application.usecase.chat.GetChatAttachmentsUseCase;
import com.ragchatbot.application.usecase.chat.GetChatHistoryUseCase;
import com.ragchatbot.application.usecase.chat.GetConversationsUseCase;
import com.ragchatbot.application.usecase.chat.SendMessageUseCase;
import com.ragchatbot.application.usecase.chat.UploadChatAttachmentUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetConversationsUseCase getConversationsUseCase;
    private final UploadChatAttachmentUseCase uploadChatAttachmentUseCase;
    private final GetChatAttachmentsUseCase getChatAttachmentsUseCase;

    public ChatController(
            SendMessageUseCase sendMessageUseCase,
            GetChatHistoryUseCase getChatHistoryUseCase,
            GetConversationsUseCase getConversationsUseCase,
            UploadChatAttachmentUseCase uploadChatAttachmentUseCase,
            GetChatAttachmentsUseCase getChatAttachmentsUseCase
    ) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.getChatHistoryUseCase = getChatHistoryUseCase;
        this.getConversationsUseCase = getConversationsUseCase;
        this.uploadChatAttachmentUseCase = uploadChatAttachmentUseCase;
        this.getChatAttachmentsUseCase = getChatAttachmentsUseCase;
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

    @PostMapping("/attachments")
    public ResponseEntity<ChatAttachmentUploadResponse> uploadAttachment(
            @RequestParam(required = false) String sessionId,
            @RequestParam("file") MultipartFile file
    ) {
        ChatAttachmentUploadResponse response = uploadChatAttachmentUseCase.execute(sessionId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/attachments/{sessionId}")
    public ResponseEntity<List<ChatAttachmentItemResponse>> getAttachments(@PathVariable String sessionId) {
        return ResponseEntity.ok(getChatAttachmentsUseCase.execute(sessionId));
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
    public List<ChatHistoryMessageDto> getChatHistory(

            @Parameter(
                    description = "Session ID của cuộc hội thoại",
                    example = "session-001"
            )
            @PathVariable
            String sessionId

    ) {
        return getChatHistoryUseCase.execute(sessionId);
    }

    @Operation(
            summary = "Get conversations",
            description = "Láº¥y danh sÃ¡ch cuá»™c há»™i thoáº¡i theo thá»© tá»± cáº­p nháº­t gáº§n nháº¥t."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Get conversations successfully"
            )
    })
    @GetMapping("/conversations")
    public List<ConversationSummaryDto> getConversations() {
        return getConversationsUseCase.execute();
    }
}

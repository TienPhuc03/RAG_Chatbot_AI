package com.ragchatbot.api.error;

import com.ragchatbot.domain.exception.ChunkingException;
import com.ragchatbot.domain.exception.DocumentParseException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi parse document.
     */
    @ExceptionHandler(DocumentParseException.class)
    public ResponseEntity<ErrorResponse> handleDocumentParse(
            DocumentParseException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Document Parse Error",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    /**
     * Xử lý lỗi chunking document.
     */
    @ExceptionHandler(ChunkingException.class)
    public ResponseEntity<ErrorResponse> handleChunking(
            ChunkingException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Chunking Error",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    /**
     * Xử lý lỗi Bean Validation.
     *
     * Ví dụ:
     * - question rỗng
     * - sessionId rỗng
     * - sessionId vượt quá giới hạn ký tự
     *
     * Trả về HTTP 400 cùng danh sách field bị lỗi.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity.badRequest().body(errors);
    }
}
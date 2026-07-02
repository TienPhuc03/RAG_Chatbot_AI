package com.ragchatbot.api.error;

import com.google.genai.errors.ClientException;
import com.ragchatbot.domain.exception.ChunkingException;
import com.ragchatbot.domain.exception.DocumentParseException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern RETRY_AFTER_SECONDS_PATTERN =
            Pattern.compile("Please retry in ([0-9]+(?:\\.[0-9]+)?)s", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(DocumentParseException.class)
    public ResponseEntity<ErrorResponse> handleDocumentParse(
            DocumentParseException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Document Parse Error",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ChunkingException.class)
    public ResponseEntity<ErrorResponse> handleChunking(
            ChunkingException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Chunking Error",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponse> handleGeminiClientException(
            ClientException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("429")
                ? HttpStatus.TOO_MANY_REQUESTS
                : HttpStatus.BAD_GATEWAY;

        return buildErrorResponse(
                status,
                status == HttpStatus.TOO_MANY_REQUESTS ? "Upstream Rate Limit" : "Upstream AI Error",
                buildGeminiMessage(ex.getMessage()),
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Da xay ra loi ngoai du kien. Vui long thu lai sau.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        error,
                        message == null || message.isBlank() ? error : message,
                        request.getRequestURI()
                )
        );
    }

    private String buildGeminiMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isBlank()) {
            return "Dich vu AI dang tam thoi qua tai. Vui long thu lai sau.";
        }

        Matcher matcher = RETRY_AFTER_SECONDS_PATTERN.matcher(originalMessage);
        if (matcher.find()) {
            long retryAfterSeconds = Math.max(1L, Math.round(Double.parseDouble(matcher.group(1))));
            return "Gemini dang het quota tam thoi. Hay thu lai sau khoang " + retryAfterSeconds + " giay.";
        }

        String normalized = originalMessage.toLowerCase();
        if (normalized.contains("quota") || normalized.contains("rate limit")) {
            return "Gemini dang het quota tam thoi. Vui long thu lai sau hoac chuyen sang Ollama local.";
        }

        return originalMessage;
    }
}

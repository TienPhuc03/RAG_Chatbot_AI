package com.ragchatbot.api.error;

import com.ragchatbot.domain.exception.ChunkingException;
import com.ragchatbot.domain.exception.DocumentParseException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

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
}
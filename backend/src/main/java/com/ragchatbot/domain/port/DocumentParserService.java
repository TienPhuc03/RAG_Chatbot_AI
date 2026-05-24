package com.ragchatbot.domain.port;

public interface DocumentParserService {

    ParsedDocument parse(byte[] content, String fileName, String contentType);
}

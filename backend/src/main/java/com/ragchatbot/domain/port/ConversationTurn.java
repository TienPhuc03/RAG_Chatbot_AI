package com.ragchatbot.domain.port;

import com.ragchatbot.domain.enums.MessageRole;

public record ConversationTurn(MessageRole role, String content) {
}

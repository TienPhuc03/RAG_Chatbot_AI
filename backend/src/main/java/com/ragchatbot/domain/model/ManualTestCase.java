package com.ragchatbot.domain.model;

import java.util.List;

public record ManualTestCase(
        String id,
        String question,
        String expectedSource,
        List<String> expectedKeywords
) {}
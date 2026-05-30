package com.ragchatbot.domain.model;

public record TestCase(String id,
                       String question,
                       String groundTruth,
                       String category) { }

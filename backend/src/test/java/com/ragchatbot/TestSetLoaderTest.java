package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.infrastructure.benchmark.TestSetLoader;
import org.junit.jupiter.api.Test;

class TestSetLoaderTest {

    @Test
    void loadsExactlyFiftyTestCases() {
        TestSetLoader loader = new TestSetLoader(new ObjectMapper());
        assertThat(loader.loadTestCases()).hasSize(50);
    }
}

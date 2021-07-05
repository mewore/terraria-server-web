package io.github.mewore.tsw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.github.mewore.tsw.config.TestConfig;

@Import(TestConfig.class)
@SpringBootTest("spring.h2.console.enabled=true")
class ApplicationIT {

    @Test
    void testRun() {
        // The application has started successfully
    }
}

package com.werkpilot;

import com.werkpilot.support.PostgreSqlTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WerkPilotApplicationTests extends PostgreSqlTestContainerSupport {

    @Test
    void contextLoads() {
    }
}

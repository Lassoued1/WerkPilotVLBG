package com.werkpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ObservabilityConfigurationTest {

    @Test
    void pilotProfileUsesStructuredJsonConsoleLoggingWithTraceAndUserFields() {
        Properties properties = loadPilotProperties();
        String consolePattern = properties.getProperty("logging.pattern.console");

        assertNotNull(consolePattern);
        assertTrue(consolePattern.startsWith("{\"timestamp\""));
        assertTrue(consolePattern.contains("\"level\""));
        assertTrue(consolePattern.contains("\"service\""));
        assertTrue(consolePattern.contains("\"traceId\""));
        assertTrue(consolePattern.contains("\"userId\""));
        assertTrue(consolePattern.contains("\"message\""));
    }

    @Test
    void pilotProfileOnlyExposesHealthEndpoint() {
        Properties properties = loadPilotProperties();

        assertEquals("health", properties.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("true", properties.getProperty("management.endpoint.health.probes.enabled"));
    }

    private static Properties loadPilotProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-pilot.yml"));
        Properties properties = factory.getObject();
        assertNotNull(properties);
        return properties;
    }
}

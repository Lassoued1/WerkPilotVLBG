package com.werkpilot.shared.seed;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final boolean enabled;
    private final SeedDataContract seedDataContract;
    private final AtomicBoolean executed = new AtomicBoolean(false);

    public SeedDataInitializer(
            @Value("${werkpilot.seed.enabled:false}") boolean enabled,
            SeedDataContract seedDataContract) {
        this.enabled = enabled;
        this.seedDataContract = seedDataContract;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOGGER.info("Seed data initialization skipped because werkpilot.seed.enabled is false");
            return;
        }

        executed.set(true);
        LOGGER.info("Seed data contract loaded with {} references", seedDataContract.references().size());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasExecuted() {
        return executed.get();
    }
}

package com.werkpilot.identity.application;

import com.werkpilot.identity.application.port.UserAccountPort;
import com.werkpilot.identity.domain.UserRole;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoAdminInitializer implements ApplicationRunner {

    private final boolean enabled;
    private final String email;
    private final String password;
    private final UserAccountPort userAccountPort;
    private final PasswordEncoder passwordEncoder;

    public DemoAdminInitializer(
            @Value("${werkpilot.identity.demo-admin.enabled:false}") boolean enabled,
            @Value("${werkpilot.identity.demo-admin.email}") String email,
            @Value("${werkpilot.identity.demo-admin.password}") String password,
            UserAccountPort userAccountPort,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
        this.password = password;
        this.userAccountPort = userAccountPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || userAccountPort.findByEmail(email).isPresent()) {
            return;
        }

        userAccountPort.createUser(
                email,
                "WerkPilot Admin",
                passwordEncoder.encode(password),
                Set.of(UserRole.ADMIN));
    }
}

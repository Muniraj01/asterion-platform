package com.asterion.auth.infrastructure.security;

import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    private final boolean enabled;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            @Value("${asterion.security.bootstrap-admin.enabled:false}")
            boolean enabled,
            @Value("${asterion.security.bootstrap-admin.email:}")
            String email,
            @Value("${asterion.security.bootstrap-admin.password:}")
            String password
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!enabled) {
            return;
        }

        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap admin is enabled but email/password is not configured"
            );
        }

        EmailAddress adminEmail = new EmailAddress(email);

        var existingUser = userRepository.findByEmail(adminEmail);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            if (!user.hasRole(Role.ADMIN)) {
                user.addRole(Role.ADMIN);
                userRepository.save(user);
            }

            return;
        }

        User admin = User.register(
                adminEmail,
                passwordHasher.hash(password)
        );

        admin.addRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
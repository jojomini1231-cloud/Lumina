package com.lumina.config;

import com.lumina.entity.User;
import com.lumina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class InitDataConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitDataConfig.class);

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        User existingAdmin = userService.lambdaQuery()
                .eq(User::getUsername, "admin")
                .one();

        if (existingAdmin == null) {
            String password = generateRandomPassword();
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(password));
            userService.save(admin);
            log.warn("========================================");
            log.warn("  INITIAL ADMIN CREDENTIALS");
            log.warn("  Username: admin");
            log.warn("  Password: {}", password);
            log.warn("  Please change this password immediately!");
            log.warn("========================================");
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
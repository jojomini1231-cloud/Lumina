package com.lumina.config;

import com.lumina.entity.User;
import com.lumina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class InitDataConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitDataConfig.class);

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已经存在 admin 用户
        User existingAdmin = userService.lambdaQuery()
                .eq(User::getUsername, "admin")
                .one();

        if (existingAdmin == null) {
            // 创建管理员用户
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            userService.save(admin);
            log.info("Created default admin user: admin/admin123");
        }
    }
}
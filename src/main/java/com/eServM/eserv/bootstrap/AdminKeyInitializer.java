package com.eServM.eserv.bootstrap;

import com.eServM.eserv.model.AdminApiKey;
import com.eServM.eserv.repository.AdminApiKeyRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminKeyInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminKeyInitializer.class);

    private static final List<String> DEFAULT_KEYS = List.of(
            "ADMIN-KEY-1-20251230",
            "ADMIN-KEY-2-20251230",
            "ADMIN-KEY-3-20251230");

    @Bean
    CommandLineRunner seedAdminKeys(AdminApiKeyRepository repository) {
        return args -> {
            for (String key : DEFAULT_KEYS) {
                repository.findByKeyValueAndActiveTrue(key).orElseGet(() -> {
                    AdminApiKey apiKey = new AdminApiKey();
                    apiKey.setKeyValue(key);
                    return repository.save(apiKey);
                });
            }
            log.info("管理员密钥已初始化: {}", DEFAULT_KEYS);
        };
    }
}

package com.eServM.eserv.service;

import com.eServM.eserv.repository.AdminApiKeyRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminKeyService {

    private final AdminApiKeyRepository adminApiKeyRepository;

    public AdminKeyService(AdminApiKeyRepository adminApiKeyRepository) {
        this.adminApiKeyRepository = adminApiKeyRepository;
    }

    public boolean isValid(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return adminApiKeyRepository.findByKeyValueAndActiveTrue(key.trim()).isPresent();
    }
}

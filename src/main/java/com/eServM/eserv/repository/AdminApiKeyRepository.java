package com.eServM.eserv.repository;

import com.eServM.eserv.model.AdminApiKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminApiKeyRepository extends JpaRepository<AdminApiKey, Long> {
    Optional<AdminApiKey> findByKeyValueAndActiveTrue(String keyValue);
}

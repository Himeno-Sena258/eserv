package com.eServM.eserv.repository;

import com.eServM.eserv.model.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUid(UUID uid);
    Optional<Customer> findByUserUsername(String username);
}

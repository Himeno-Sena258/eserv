package com.eServM.eserv.repository;

import com.eServM.eserv.model.CustomerOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByUid(UUID uid);
    List<CustomerOrder> findByCustomerUserUsername(String username);
}

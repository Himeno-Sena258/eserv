package com.eServM.eserv.repository;

import com.eServM.eserv.model.OrderNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderNoteRepository extends JpaRepository<OrderNote, UUID> {

    Optional<OrderNote> findByUid(UUID uid);

    @Query("select n from OrderNote n where n.order.uid = :orderUid")
    List<OrderNote> findByOrderUid(@Param("orderUid") UUID orderUid);

    @Query("select n from OrderNote n where n.order.customer.user.username = :username")
    List<OrderNote> findByOwnerUsername(@Param("username") String username);
}

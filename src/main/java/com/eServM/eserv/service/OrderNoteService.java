package com.eServM.eserv.service;

import com.eServM.eserv.dto.OrderNoteRequest;
import com.eServM.eserv.dto.OrderNoteResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ResourceNotFoundException;
import com.eServM.eserv.model.CustomerOrder;
import com.eServM.eserv.model.OrderNote;
import com.eServM.eserv.repository.CustomerOrderRepository;
import com.eServM.eserv.repository.OrderNoteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderNoteService {

    private final OrderNoteRepository orderNoteRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public OrderNoteService(OrderNoteRepository orderNoteRepository, CustomerOrderRepository customerOrderRepository) {
        this.orderNoteRepository = orderNoteRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    public OrderNoteResponse create(OrderNoteRequest request) {
        CustomerOrder order = fetchOrder(request.orderUid());
        OrderNote note = new OrderNote();
        note.setOrder(order);
        note.setMessage(request.message());
        return toResponse(orderNoteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<OrderNoteResponse> findAll(String orderUid) {
        if (orderUid == null || orderUid.isBlank()) {
            return orderNoteRepository.findAll().stream().map(this::toResponse).toList();
        }
        UUID parsed = parse(orderUid);
        return orderNoteRepository.findByOrderUid(parsed).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderNoteResponse findByUid(String uid) {
        return toResponse(fetchNote(uid));
    }

    public OrderNoteResponse update(String uid, OrderNoteRequest request) {
        OrderNote note = fetchNote(uid);
        CustomerOrder order = fetchOrder(request.orderUid());
        note.setOrder(order);
        note.setMessage(request.message());
        return toResponse(orderNoteRepository.save(note));
    }

    public void delete(String uid) {
        OrderNote note = fetchNote(uid);
        orderNoteRepository.delete(note);
    }

    private OrderNote fetchNote(String uid) {
        UUID parsed = parse(uid);
        return orderNoteRepository.findByUid(parsed)
                .orElseThrow(() -> new ResourceNotFoundException("未找到订单备注: " + uid));
    }

    private CustomerOrder fetchOrder(String uid) {
        UUID parsed = parse(uid);
        return customerOrderRepository.findByUid(parsed)
                .orElseThrow(() -> new ResourceNotFoundException("未找到订单: " + uid));
    }

    private UUID parse(String uid) {
        try {
            return UUID.fromString(uid);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("无效的UID: " + uid);
        }
    }

    private OrderNoteResponse toResponse(OrderNote note) {
        return new OrderNoteResponse(
                note.getUid().toString(),
                note.getOrder().getUid().toString(),
                note.getOrder().getSummary(),
                note.getMessage(),
                note.getCreatedAt());
    }
}

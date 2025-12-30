package com.eServM.eserv.service;

import com.eServM.eserv.dto.OrderNoteRequest;
import com.eServM.eserv.dto.OrderNoteResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ForbiddenException;
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

    public OrderNoteResponse create(String role, String username, OrderNoteRequest request) {
        CustomerOrder order = fetchOrder(request.orderUid());
        if (!"admin".equals(role)) {
            String owner = order.getCustomer() != null && order.getCustomer().getUser() != null
                    ? order.getCustomer().getUser().getUsername() : null;
            if (owner == null || !owner.equals(username)) {
                throw new ForbiddenException("仅可为自己的订单添加备注");
            }
        }
        OrderNote note = new OrderNote();
        note.setOrder(order);
        note.setMessage(request.message());
        return toResponse(orderNoteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<OrderNoteResponse> findAll(String role, String username, String orderUid) {
        if ("admin".equals(role)) {
            if (orderUid == null || orderUid.isBlank()) {
                return orderNoteRepository.findAll().stream().map(this::toResponse).toList();
            }
            UUID parsed = parse(orderUid);
            return orderNoteRepository.findByOrderUid(parsed).stream().map(this::toResponse).toList();
        } else {
            if (orderUid == null || orderUid.isBlank()) {
                return orderNoteRepository.findByOwnerUsername(username).stream().map(this::toResponse).toList();
            }
            CustomerOrder order = fetchOrder(orderUid);
            String owner = order.getCustomer() != null && order.getCustomer().getUser() != null
                    ? order.getCustomer().getUser().getUsername() : null;
            if (owner == null || !owner.equals(username)) {
                throw new ForbiddenException("无权查看该订单的备注");
            }
            UUID parsed = parse(orderUid);
            return orderNoteRepository.findByOrderUid(parsed).stream().map(this::toResponse).toList();
        }
    }

    @Transactional(readOnly = true)
    public OrderNoteResponse findByUid(String role, String username, String uid) {
        OrderNote note = fetchNote(uid);
        if (!"admin".equals(role)) {
            String owner = note.getOrder() != null && note.getOrder().getCustomer() != null
                    && note.getOrder().getCustomer().getUser() != null
                    ? note.getOrder().getCustomer().getUser().getUsername() : null;
            if (owner == null || !owner.equals(username)) {
                throw new ForbiddenException("无权访问该订单备注");
            }
        }
        return toResponse(note);
    }

    public OrderNoteResponse update(String role, String username, String uid, OrderNoteRequest request) {
        OrderNote note = fetchNote(uid);
        CustomerOrder targetOrder = fetchOrder(request.orderUid());
        if (!"admin".equals(role)) {
            String ownerOfExisting = note.getOrder() != null && note.getOrder().getCustomer() != null
                    && note.getOrder().getCustomer().getUser() != null
                    ? note.getOrder().getCustomer().getUser().getUsername() : null;
            String ownerOfTarget = targetOrder.getCustomer() != null && targetOrder.getCustomer().getUser() != null
                    ? targetOrder.getCustomer().getUser().getUsername() : null;
            if (!username.equals(ownerOfExisting) || !username.equals(ownerOfTarget)) {
                throw new ForbiddenException("仅可修改属于自己的订单备注");
            }
        }
        note.setOrder(targetOrder);
        note.setMessage(request.message());
        return toResponse(orderNoteRepository.save(note));
    }

    public void delete(String role, String username, String uid) {
        OrderNote note = fetchNote(uid);
        if (!"admin".equals(role)) {
            String owner = note.getOrder() != null && note.getOrder().getCustomer() != null
                    && note.getOrder().getCustomer().getUser() != null
                    ? note.getOrder().getCustomer().getUser().getUsername() : null;
            if (owner == null || !owner.equals(username)) {
                throw new ForbiddenException("仅可删除属于自己的订单备注");
            }
        }
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

package com.eServM.eserv.service;

import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ResourceNotFoundException;
import com.eServM.eserv.model.Customer;
import com.eServM.eserv.model.CustomerOrder;
import com.eServM.eserv.repository.CustomerOrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerService customerService;

    public OrderService(CustomerOrderRepository customerOrderRepository, CustomerService customerService) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerService = customerService;
    }

    public OrderResponse create(OrderRequest request) {
        Customer customer = customerService.fetchCustomer(request.customerUid());
        CustomerOrder order = new CustomerOrder();
        order.setSummary(request.summary());
        order.setProductName(request.productName());
        order.setCustomer(customer);
        order.setOrderTime(resolveOrderTime(request));
        return toResponse(customerOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return customerOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findByUid(String uid) {
        return toResponse(fetchOrder(uid));
    }

    public OrderResponse update(String uid, OrderRequest request) {
        CustomerOrder order = fetchOrder(uid);
        Customer customer = customerService.fetchCustomer(request.customerUid());
        order.setSummary(request.summary());
        order.setProductName(request.productName());
        order.setCustomer(customer);
        order.setOrderTime(resolveOrderTime(request));
        return toResponse(customerOrderRepository.save(order));
    }

    public void delete(String uid) {
        CustomerOrder order = fetchOrder(uid);
        customerOrderRepository.delete(order);
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

    private OffsetDateTime resolveOrderTime(OrderRequest request) {
        return request.orderTime() != null ? request.orderTime() : OffsetDateTime.now();
    }

    private OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getUid().toString(),
                order.getSummary(),
                order.getProductName(),
                order.getCustomer().getUid().toString(),
                order.getCustomer().getName(),
                order.getOrderTime());
    }
}

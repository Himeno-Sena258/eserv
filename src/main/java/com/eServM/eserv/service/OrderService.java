package com.eServM.eserv.service;

import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ForbiddenException;
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

    public OrderResponse create(String role, String username, OrderRequest request) {
        Customer customer = customerService.fetchCustomer(request.customerUid());
        if (!"admin".equals(role)) {
            if (customer.getUser() == null || customer.getUser().getUsername() == null
                    || !customer.getUser().getUsername().equals(username)) {
                throw new ForbiddenException("仅可为自己的客户创建订单");
            }
        }
        CustomerOrder order = new CustomerOrder();
        order.setSummary(request.summary());
        order.setProductName(request.productName());
        order.setCustomer(customer);
        order.setOrderTime(resolveOrderTime(request));
        return toResponse(customerOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(String role, String username) {
        if ("admin".equals(role)) {
            return customerOrderRepository.findAll().stream().map(this::toResponse).toList();
        }
        return customerOrderRepository.findByCustomerUserUsername(username).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findByUid(String role, String username, String uid) {
        CustomerOrder order = fetchOrder(uid);
        if (!"admin".equals(role)) {
            if (order.getCustomer() == null || order.getCustomer().getUser() == null
                    || order.getCustomer().getUser().getUsername() == null
                    || !order.getCustomer().getUser().getUsername().equals(username)) {
                throw new ForbiddenException("无权访问该订单");
            }
        }
        return toResponse(order);
    }

    public OrderResponse update(String role, String username, String uid, OrderRequest request) {
        CustomerOrder order = fetchOrder(uid);
        Customer customer = customerService.fetchCustomer(request.customerUid());
        if (!"admin".equals(role)) {
            String ownerOfExisting = order.getCustomer() != null && order.getCustomer().getUser() != null
                    ? order.getCustomer().getUser().getUsername() : null;
            String ownerOfTarget = customer.getUser() != null ? customer.getUser().getUsername() : null;
            if (!username.equals(ownerOfExisting) || !username.equals(ownerOfTarget)) {
                throw new ForbiddenException("仅可修改属于自己的订单");
            }
        }
        order.setSummary(request.summary());
        order.setProductName(request.productName());
        order.setCustomer(customer);
        order.setOrderTime(resolveOrderTime(request));
        return toResponse(customerOrderRepository.save(order));
    }

    public void delete(String role, String username, String uid) {
        CustomerOrder order = fetchOrder(uid);
        if (!"admin".equals(role)) {
            String ownerOfExisting = order.getCustomer() != null && order.getCustomer().getUser() != null
                    ? order.getCustomer().getUser().getUsername() : null;
            if (!username.equals(ownerOfExisting)) {
                throw new ForbiddenException("仅可删除属于自己的订单");
            }
        }
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

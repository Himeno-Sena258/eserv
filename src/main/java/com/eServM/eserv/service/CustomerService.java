package com.eServM.eserv.service;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ResourceNotFoundException;
import com.eServM.eserv.model.Customer;
import com.eServM.eserv.repository.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setContactMethod(request.contactMethod());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByUid(String uid) {
        return toResponse(fetchCustomer(uid));
    }

    public CustomerResponse update(String uid, CustomerRequest request) {
        Customer customer = fetchCustomer(uid);
        customer.setName(request.name());
        customer.setContactMethod(request.contactMethod());
        return toResponse(customerRepository.save(customer));
    }

    public void delete(String uid) {
        Customer customer = fetchCustomer(uid);
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public Customer fetchCustomer(String uid) {
        UUID parsed = parse(uid);
        return customerRepository.findByUid(parsed)
                .orElseThrow(() -> new ResourceNotFoundException("未找到客户: " + uid));
    }

    private UUID parse(String uid) {
        try {
            return UUID.fromString(uid);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("无效的UID: " + uid);
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getUid().toString(),
                customer.getName(),
                customer.getContactMethod(),
                customer.getCreatedAt());
    }
}

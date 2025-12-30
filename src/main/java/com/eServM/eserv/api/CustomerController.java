package com.eServM.eserv.api;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(HttpServletRequest httpRequest, @Valid @RequestBody CustomerRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return customerService.create(role, username, request);
    }

    @GetMapping
    public List<CustomerResponse> findAll(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return customerService.findAll(role, username);
    }

    @GetMapping("/{uid}")
    public CustomerResponse findOne(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return customerService.findByUid(role, username, uid);
    }

    @PutMapping("/{uid}")
    public CustomerResponse update(HttpServletRequest httpRequest, @PathVariable String uid, @Valid @RequestBody CustomerRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return customerService.update(role, username, uid, request);
    }

    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        customerService.delete(role, username, uid);
    }
}

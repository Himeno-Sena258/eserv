package com.eServM.eserv.api;

import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.service.OrderService;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(HttpServletRequest httpRequest, @Valid @RequestBody OrderRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderService.create(role, username, request);
    }

    @GetMapping
    public List<OrderResponse> findAll(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderService.findAll(role, username);
    }

    @GetMapping("/{uid}")
    public OrderResponse findOne(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderService.findByUid(role, username, uid);
    }

    @PutMapping("/{uid}")
    public OrderResponse update(HttpServletRequest httpRequest, @PathVariable String uid, @Valid @RequestBody OrderRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderService.update(role, username, uid, request);
    }

    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        orderService.delete(role, username, uid);
    }
}

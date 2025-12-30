package com.eServM.eserv.api;

import com.eServM.eserv.dto.OrderNoteRequest;
import com.eServM.eserv.dto.OrderNoteResponse;
import com.eServM.eserv.service.OrderNoteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-notes")
public class OrderNoteController {

    private final OrderNoteService orderNoteService;

    public OrderNoteController(OrderNoteService orderNoteService) {
        this.orderNoteService = orderNoteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderNoteResponse create(HttpServletRequest httpRequest, @Valid @RequestBody OrderNoteRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderNoteService.create(role, username, request);
    }

    @GetMapping
    public List<OrderNoteResponse> findAll(HttpServletRequest httpRequest, @RequestParam(required = false) String orderUid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderNoteService.findAll(role, username, orderUid);
    }

    @GetMapping("/{uid}")
    public OrderNoteResponse findOne(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderNoteService.findByUid(role, username, uid);
    }

    @PutMapping("/{uid}")
    public OrderNoteResponse update(HttpServletRequest httpRequest, @PathVariable String uid, @Valid @RequestBody OrderNoteRequest request) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        return orderNoteService.update(role, username, uid, request);
    }

    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest httpRequest, @PathVariable String uid) {
        String role = (String) httpRequest.getAttribute("currentRole");
        String username = (String) httpRequest.getAttribute("currentUsername");
        orderNoteService.delete(role, username, uid);
    }
}

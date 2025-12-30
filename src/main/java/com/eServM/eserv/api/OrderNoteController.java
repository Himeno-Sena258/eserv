package com.eServM.eserv.api;

import com.eServM.eserv.dto.OrderNoteRequest;
import com.eServM.eserv.dto.OrderNoteResponse;
import com.eServM.eserv.service.OrderNoteService;
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
    public OrderNoteResponse create(@Valid @RequestBody OrderNoteRequest request) {
        return orderNoteService.create(request);
    }

    @GetMapping
    public List<OrderNoteResponse> findAll(@RequestParam(required = false) String orderUid) {
        return orderNoteService.findAll(orderUid);
    }

    @GetMapping("/{uid}")
    public OrderNoteResponse findOne(@PathVariable String uid) {
        return orderNoteService.findByUid(uid);
    }

    @PutMapping("/{uid}")
    public OrderNoteResponse update(@PathVariable String uid, @Valid @RequestBody OrderNoteRequest request) {
        return orderNoteService.update(uid, request);
    }

    @DeleteMapping("/{uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String uid) {
        orderNoteService.delete(uid);
    }
}

package com.eServM.eserv.api;

import com.eServM.eserv.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/register")
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    /** 用户注册，返回用户信息 */
    @PostMapping
    public ResponseEntity<UserResponse> register(@RequestBody UserRegisterRequest request) {
        if (request == null || request.username == null || request.password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        UserResponse resp = userService.register(request.username.trim(), request.password);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    public static class UserRegisterRequest {
        public String username;
        public String password;
    }

    public static class UserResponse {
        public Long id;
        public String username;
        public boolean active;
        public UserResponse(Long id, String username, boolean active) {
            this.id = id;
            this.username = username;
            this.active = active;
        }
    }
}

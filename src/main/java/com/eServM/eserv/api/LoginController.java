package com.eServM.eserv.api;

import com.eServM.eserv.security.JwtService;
import com.eServM.eserv.service.AdminKeyService;
import com.eServM.eserv.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final AdminKeyService adminKeyService;
    private final JwtService jwtService;
    private final UserService userService;

    public LoginController(AdminKeyService adminKeyService, JwtService jwtService, UserService userService) {
        this.adminKeyService = adminKeyService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /** 管理员登录，返回 JWT */
    @PostMapping("/admin")
    public ResponseEntity<TokenResponse> loginAdmin(@RequestBody AdminLoginRequest request) {
        if (request == null || request.adminKey == null || !adminKeyService.isValid(request.adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.createTokenForAdminKey(request.adminKey.trim());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    /** 用户登录，返回 JWT */
    @PostMapping
    public ResponseEntity<TokenResponse> loginUser(@RequestBody UserLoginRequest request) {
        if (request == null || request.username == null || request.password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String token = userService.loginAndIssueToken(request.username.trim(), request.password);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public static class AdminLoginRequest {
        public String adminKey;
    }

    public static class UserLoginRequest {
        public String username;
        public String password;
    }

    public static class TokenResponse {
        public String token;
        public TokenResponse(String token) {
            this.token = token;
        }
    }
}

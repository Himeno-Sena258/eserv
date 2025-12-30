package com.eServM.eserv.api;

import com.eServM.eserv.security.JwtService;
import com.eServM.eserv.service.AdminKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminKeyService adminKeyService;
    private final JwtService jwtService;

    public AuthController(AdminKeyService adminKeyService, JwtService jwtService) {
        this.adminKeyService = adminKeyService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.adminKey == null || !adminKeyService.isValid(request.adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.createTokenForAdminKey(request.adminKey.trim());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public static class LoginRequest {
        public String adminKey;
    }

    public static class TokenResponse {
        public String token;
        public TokenResponse(String token) {
            this.token = token;
        }
    }
}

package com.eServM.eserv.service;

import com.eServM.eserv.api.RegisterController.UserResponse;
import com.eServM.eserv.model.User;
import com.eServM.eserv.repository.UserRepository;
import com.eServM.eserv.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /** 注册新用户 */
    public UserResponse register(String username, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(u -> {
            throw new IllegalStateException("用户名已存在");
        });
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.isActive());
    }

    /** 用户登录并签发令牌 */
    public String loginAndIssueToken(String username, String rawPassword) {
        User user = userRepository.findByUsernameAndActiveTrue(username).orElse(null);
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return null;
        }
        return jwtService.createTokenForUser(user.getUsername());
    }
}

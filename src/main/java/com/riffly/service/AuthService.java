package com.riffly.service;

import com.riffly.config.JwtProperties;
import com.riffly.dto.AuthDTO;
import com.riffly.exception.RifflyException;
import com.riffly.model.User;
import com.riffly.repository.UserRepository;
import com.riffly.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final JwtProperties         jwtProperties;
    private final AuthenticationManager authManager;

    public AuthDTO.TokenResponse register(AuthDTO.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw RifflyException.conflict(
                    "Username '" + req.getUsername() + "' is already taken");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .displayName(req.getDisplayName() != null
                        ? req.getDisplayName()
                        : req.getUsername())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        return buildTokenResponse(token, user);
    }

    public AuthDTO.TokenResponse login(AuthDTO.LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getUsername(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw RifflyException.badRequest("Invalid username or password");
        }

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> RifflyException.notFound("User", -1L));

        String token = jwtService.generateToken(user.getUsername());
        return buildTokenResponse(token, user);
    }

    private AuthDTO.TokenResponse buildTokenResponse(String token, User user) {
        return AuthDTO.TokenResponse.builder()
                .token(token)
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .expiresInMs(jwtProperties.getExpirationMs())
                .build();
    }
}

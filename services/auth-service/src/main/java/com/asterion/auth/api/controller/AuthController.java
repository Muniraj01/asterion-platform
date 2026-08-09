package com.asterion.auth.api.controller;

import com.asterion.auth.api.request.LoginRequest;
import com.asterion.auth.api.response.LoginResponse;
import com.asterion.auth.application.port.in.AuthenticateUserCommand;
import com.asterion.auth.application.port.in.AuthenticateUserUseCase;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.infrastructure.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            AuthenticateUserUseCase authenticateUserUseCase,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        User user = authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(
                        request.email(),
                        request.password()
                )
        );

        String token = jwtTokenProvider.generateToken(user);

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        "Bearer",
                        jwtTokenProvider.expirationSeconds()
                )
        );
    }
}
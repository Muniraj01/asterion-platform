package com.asterion.auth.api.controller;

import com.asterion.auth.api.request.LoginRequest;
import com.asterion.auth.api.request.LogoutRequest;
import com.asterion.auth.api.request.RefreshTokenRequest;
import com.asterion.auth.api.response.LoginResponse;
import com.asterion.auth.application.port.in.*;
import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            AuthenticateUserUseCase authenticateUserUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            LogoutUseCase logoutUseCase,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
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

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshAccessTokenUseCase.issue(user);

        return ResponseEntity.ok(
                new LoginResponse(
                        accessToken,
                        refreshToken,
                        "Bearer",
                        jwtTokenProvider.accessTokenExpirationSeconds()
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        TokenPair tokens =
                refreshAccessTokenUseCase.refresh(
                        new RefreshAccessTokenCommand(
                                request.refreshToken()
                        )
                );

        return ResponseEntity.ok(
                new LoginResponse(
                        tokens.accessToken(),
                        tokens.refreshToken(),
                        "Bearer",
                        jwtTokenProvider.accessTokenExpirationSeconds()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        logoutUseCase.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}

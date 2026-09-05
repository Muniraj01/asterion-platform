package com.asterion.auth.api.controller;

import com.asterion.auth.api.response.InternalUserResponse;
import com.asterion.auth.application.port.in.GetInternalUserUseCase;
import com.asterion.auth.application.port.in.InternalUserResult;
import com.asterion.auth.domain.model.UserId;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final GetInternalUserUseCase getInternalUserUseCase;

    public InternalUserController(GetInternalUserUseCase getInternalUserUseCase) {
        this.getInternalUserUseCase = getInternalUserUseCase;
    }

    @PreAuthorize("hasAuthority('INTERNAL_USER_READ')")
    @GetMapping("/{userId}")
    public InternalUserResponse getUser(@PathVariable UUID userId) {
        InternalUserResult result = getInternalUserUseCase.getUser(new UserId(userId));
        return new InternalUserResponse(
                result.userId().value(),
                result.email(),
                result.active(),
                result.createdAt(),
                result.roles()
        );
    }
}
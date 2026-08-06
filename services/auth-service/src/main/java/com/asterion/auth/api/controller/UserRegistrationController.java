package com.asterion.auth.api.controller;

import com.asterion.auth.api.request.RegisterUserRequest;
import com.asterion.auth.api.response.UserResponse;
import com.asterion.auth.application.command.RegisterUserCommand;
import com.asterion.auth.application.port.in.RegisterUserUseCase;
import com.asterion.auth.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserRegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public UserRegistrationController(
            RegisterUserUseCase registerUserUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {

        User user = registerUserUseCase.register(
                new RegisterUserCommand(
                        request.email(),
                        request.password()
                )
        );

        return new UserResponse(
                user.id().toString(),
                user.email().value(),
                user.createdAt(),
                user.isActive()
        );
    }
}
package com.asterion.auth.api.controller;

import com.asterion.auth.api.response.CurrentUserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public CurrentUserResponse currentUser(
            Authentication authentication
    ) {

        return new CurrentUserResponse(
                authentication.getName(),
                authentication.isAuthenticated()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminEndpoint() {
        return "Admin access granted";
    }
}
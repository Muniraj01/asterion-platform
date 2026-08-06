package com.asterion.auth.api.controller;

import com.asterion.auth.api.response.CurrentUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                new CurrentUserResponse(
                        authentication.getName(),
                        authentication.isAuthenticated()
                )
        );
    }
}
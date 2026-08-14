package com.asterion.auth.application.port.in;

import com.asterion.auth.domain.model.User;

public interface RefreshAccessTokenUseCase {

    TokenPair refresh(
            RefreshAccessTokenCommand command
    );

    String issue(User user);
}
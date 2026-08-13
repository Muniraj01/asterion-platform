package com.asterion.auth.application.port.in;

import com.asterion.auth.domain.model.User;

public interface RefreshAccessTokenUseCase {

    String issue(User user);

    String refresh(RefreshAccessTokenCommand command);
}
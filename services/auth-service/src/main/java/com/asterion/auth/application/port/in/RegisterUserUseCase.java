package com.asterion.auth.application.port.in;

import com.asterion.auth.domain.model.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}
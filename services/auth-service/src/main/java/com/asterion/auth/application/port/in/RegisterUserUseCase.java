package com.asterion.auth.application.port.in;

import com.asterion.auth.application.command.RegisterUserCommand;
import com.asterion.auth.domain.model.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}
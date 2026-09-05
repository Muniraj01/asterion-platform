package com.asterion.auth.application.port.in;

import com.asterion.auth.domain.model.UserId;

public interface GetInternalUserUseCase {

    InternalUserResult getUser(UserId userId);
}
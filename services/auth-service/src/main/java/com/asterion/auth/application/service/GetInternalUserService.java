package com.asterion.auth.application.service;

import com.asterion.auth.application.port.in.GetInternalUserUseCase;
import com.asterion.auth.application.port.in.InternalUserResult;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.exception.UserNotFoundException;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import org.springframework.stereotype.Service;

@Service
public class GetInternalUserService implements GetInternalUserUseCase {

    private final UserRepository userRepository;

    public GetInternalUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public InternalUserResult getUser(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return new InternalUserResult(
                user.id(),
                user.email().value(),
                user.isActive(),
                user.createdAt(),
                user.roles()
        );
    }
}
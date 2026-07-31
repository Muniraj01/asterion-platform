package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);
}
package com.asterion.auth.infrastructure.persistence.mapper;

import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import com.asterion.auth.infrastructure.persistence.entity.UserEntity;

public final class UserEntityMapper {

    private UserEntityMapper() {
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.passwordHash().value(),
                user.isActive(),
                user.createdAt()
        );
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                new UserId(entity.getId()),
                new EmailAddress(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.getCreatedAt(),
                entity.isActive()
        );
    }
}
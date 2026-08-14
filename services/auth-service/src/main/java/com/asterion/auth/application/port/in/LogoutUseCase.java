package com.asterion.auth.application.port.in;

public interface LogoutUseCase {

    void logout(String refreshToken);
}
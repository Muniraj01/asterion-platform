package com.asterion.auth.application.port.out;

public interface TokenHasher {

    String hash(String token);
}
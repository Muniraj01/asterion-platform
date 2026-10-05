package com.asterion.order.application.command;

import java.util.UUID;

public record ListOrdersCommand(
        UUID authenticatedUserId,
        int page,
        int size
) {
}
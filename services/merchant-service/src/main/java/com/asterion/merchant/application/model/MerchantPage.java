package com.asterion.merchant.application.model;

import com.asterion.merchant.domain.model.Merchant;

import java.util.List;
import java.util.Objects;

public record MerchantPage(
        List<Merchant> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public MerchantPage {
        Objects.requireNonNull(content, "content must not be null");
        if (page < 0)
            throw new IllegalArgumentException("page must not be negative");

        if (size <= 0)
            throw new IllegalArgumentException("size must be greater than zero");

        if (totalElements < 0)
            throw new IllegalArgumentException("totalElements must not be negative");

        if (totalPages < 0)
            throw new IllegalArgumentException("totalPages must not be negative");

        content = List.copyOf(content);
    }
}
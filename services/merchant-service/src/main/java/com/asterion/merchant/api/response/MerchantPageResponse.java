package com.asterion.merchant.api.response;

import com.asterion.merchant.application.model.MerchantPage;

import java.util.List;

public record MerchantPageResponse(
        List<MerchantResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static MerchantPageResponse from(MerchantPage merchantPage) {
        return new MerchantPageResponse(
                merchantPage.content()
                        .stream()
                        .map(MerchantResponse::from)
                        .toList(),
                merchantPage.page(),
                merchantPage.size(),
                merchantPage.totalElements(),
                merchantPage.totalPages()
        );
    }
}
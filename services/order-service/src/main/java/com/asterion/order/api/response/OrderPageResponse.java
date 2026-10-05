package com.asterion.order.api.response;

import com.asterion.order.application.model.OrderPage;

import java.util.List;

public record OrderPageResponse(
        List<OrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static OrderPageResponse from(OrderPage orderPage) {

        return new OrderPageResponse(
                orderPage.content()
                        .stream()
                        .map(OrderResponse::from)
                        .toList(),
                orderPage.page(),
                orderPage.size(),
                orderPage.totalElements(),
                orderPage.totalPages()
        );
    }
}
package com.asterion.merchant.api.controller;

import com.asterion.merchant.api.request.CreateMerchantRequest;
import com.asterion.merchant.api.response.MerchantResponse;
import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.infrastructure.security.MerchantUserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;

    public MerchantController(CreateMerchantUseCase createMerchantUseCase) {
        this.createMerchantUseCase = createMerchantUseCase;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> createMerchant(
            @Valid @RequestBody CreateMerchantRequest request,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Merchant merchant = createMerchantUseCase.create(
                new CreateMerchantCommand(
                        identity.userId(),
                        request.businessName(),
                        request.legalName(),
                        request.contactEmail()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MerchantResponse.from(merchant));
    }
}
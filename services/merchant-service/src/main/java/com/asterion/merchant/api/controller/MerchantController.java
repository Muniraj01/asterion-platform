package com.asterion.merchant.api.controller;

import com.asterion.merchant.api.request.CreateMerchantRequest;
import com.asterion.merchant.api.response.MerchantResponse;
import com.asterion.merchant.application.command.ActivateMerchantCommand;
import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.command.GetMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.in.ActivateMerchantUseCase;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.in.GetMerchantUseCase;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.infrastructure.security.MerchantUserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;
    private final ActivateMerchantUseCase activateMerchantUseCase;
    private final GetMerchantUseCase getMerchantUseCase;

    public MerchantController(
            CreateMerchantUseCase createMerchantUseCase,
            ActivateMerchantUseCase activateMerchantUseCase,
            GetMerchantUseCase getMerchantUseCase) {
        this.createMerchantUseCase = createMerchantUseCase;
        this.activateMerchantUseCase = activateMerchantUseCase;
        this.getMerchantUseCase = getMerchantUseCase;
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

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> getMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Merchant merchant = getMerchantUseCase.get(
                    new GetMerchantCommand(merchantId, identity.userId()));
            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{merchantId}/activate")
    public ResponseEntity<MerchantResponse> activateMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Merchant merchant = activateMerchantUseCase.activate(
                    new ActivateMerchantCommand(merchantId, identity.userId()));
            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
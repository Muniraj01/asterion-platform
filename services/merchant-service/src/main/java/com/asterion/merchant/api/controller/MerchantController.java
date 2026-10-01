package com.asterion.merchant.api.controller;

import com.asterion.merchant.api.request.CreateMerchantRequest;
import com.asterion.merchant.api.request.UpdateMerchantRequest;
import com.asterion.merchant.api.response.MerchantPageResponse;
import com.asterion.merchant.api.response.MerchantResponse;
import com.asterion.merchant.application.command.*;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.model.MerchantPage;
import com.asterion.merchant.application.port.in.*;
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
    private final SuspendMerchantUseCase suspendMerchantUseCase;
    private final ReactivateMerchantUseCase reactivateMerchantUseCase;
    private final TerminateMerchantUseCase terminateMerchantUseCase;
    private final GetMerchantUseCase getMerchantUseCase;
    private final ListMerchantsUseCase listMerchantsUseCase;
    private final UpdateMerchantUseCase updateMerchantUseCase;

    public MerchantController(
            CreateMerchantUseCase createMerchantUseCase,
            ActivateMerchantUseCase activateMerchantUseCase,
            SuspendMerchantUseCase suspendMerchantUseCase,
            ReactivateMerchantUseCase reactivateMerchantUseCase,
            TerminateMerchantUseCase terminateMerchantUseCase,
            GetMerchantUseCase getMerchantUseCase,
            ListMerchantsUseCase listMerchantsUseCase,
            UpdateMerchantUseCase updateMerchantUseCase) {

        this.createMerchantUseCase = createMerchantUseCase;
        this.activateMerchantUseCase = activateMerchantUseCase;
        this.suspendMerchantUseCase = suspendMerchantUseCase;
        this.reactivateMerchantUseCase = reactivateMerchantUseCase;
        this.terminateMerchantUseCase = terminateMerchantUseCase;
        this.getMerchantUseCase = getMerchantUseCase;
        this.listMerchantsUseCase = listMerchantsUseCase;
        this.updateMerchantUseCase = updateMerchantUseCase;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> createMerchant(
            @Valid @RequestBody CreateMerchantRequest request,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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

    @GetMapping
    public ResponseEntity<MerchantPageResponse> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (page < 0 || size <= 0)
            return ResponseEntity.badRequest().build();

        MerchantPage merchantPage = listMerchantsUseCase.list(
                new ListMerchantsCommand(identity.userId(), page, size));

        return ResponseEntity.ok(MerchantPageResponse.from(merchantPage));
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> getMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = getMerchantUseCase.get(
                    new GetMerchantCommand(merchantId, identity.userId())
            );

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

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = activateMerchantUseCase.activate(
                    new ActivateMerchantCommand(merchantId, identity.userId())
            );

            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{merchantId}/suspend")
    public ResponseEntity<MerchantResponse> suspendMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = suspendMerchantUseCase.suspend(
                    new SuspendMerchantCommand(merchantId, identity.userId())
            );

            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{merchantId}/reactivate")
    public ResponseEntity<MerchantResponse> reactivateMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = reactivateMerchantUseCase.reactivate(
                    new ReactivateMerchantCommand(merchantId, identity.userId())
            );

            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{merchantId}/terminate")
    public ResponseEntity<MerchantResponse> terminateMerchant(
            @PathVariable UUID merchantId,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = terminateMerchantUseCase.terminate(
                    new TerminateMerchantCommand(merchantId, identity.userId())
            );

            return ResponseEntity.ok(MerchantResponse.from(merchant));

        } catch (MerchantOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{merchantId}")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable UUID merchantId,
            @Valid @RequestBody UpdateMerchantRequest request,
            HttpServletRequest httpRequest) {

        MerchantUserIdentity identity = (MerchantUserIdentity) httpRequest
                .getAttribute(MerchantUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Merchant merchant = updateMerchantUseCase.update(
                    new UpdateMerchantCommand(
                            merchantId,
                            identity.userId(),
                            request.businessName(),
                            request.legalName(),
                            request.contactEmail()
                    )
            );

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
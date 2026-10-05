package com.asterion.merchant.api.controller;

import com.asterion.merchant.api.response.MerchantValidationResponse;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/merchants")
public class MerchantInternalController {

    private final MerchantRepository merchantRepository;

    public MerchantInternalController(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantValidationResponse> getMerchant(
            @PathVariable UUID merchantId) {

        Merchant merchant = merchantRepository.findById(merchantId).orElse(null);

        if (merchant == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(MerchantValidationResponse.from(merchant));
    }
}
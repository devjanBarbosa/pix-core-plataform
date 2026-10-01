package com.pixcore.domain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pixcore.domain.DTO.TransferRequest;
import com.pixcore.domain.DTO.TransferResponse;
import com.pixcore.domain.service.TransferService;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        var transaction = transferService.executeTransfer(request);

        var response = new TransferResponse(
                transaction.getId(),
                transaction.getIdempotencyKey(),
                transaction.getSourceAccountId(),
                transaction.getDestinationPixKey(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
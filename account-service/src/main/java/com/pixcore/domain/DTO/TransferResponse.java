package com.pixcore.domain.DTO;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.pixcore.domain.entity.TransactionEntity.TransactionStatus;

public record TransferResponse(
    UUID transactionId,
    String idempotencyKey,
    UUID sourceAccountId,
    String destinationPixKey,
    BigDecimal amount,
    TransactionStatus status,
    OffsetDateTime createdAt
) {
  
}

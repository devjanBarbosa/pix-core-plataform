package com.pixcore.domain.DTO;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
    String idempotencyKey,
    UUID sourceAccountId,
    String destinationPixKey,
    BigDecimal amount
)  {
  
}

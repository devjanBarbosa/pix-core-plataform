package com.pixcore.domain.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pixcore.domain.DTO.TransferRequest;
import com.pixcore.domain.entity.LedgerEntryEntity;
import com.pixcore.domain.entity.TransactionEntity;
import com.pixcore.domain.entity.LedgerEntryEntity.EntryType;
import com.pixcore.domain.entity.TransactionEntity.TransactionStatus;
import com.pixcore.domain.repository.AccountRepository;
import com.pixcore.domain.repository.LedgerEntryRepository;
import com.pixcore.domain.repository.TransactionRepository;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public TransactionEntity executeTransfer(TransferRequest request) {

        var existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingTransaction.isPresent()) {
            log.info("Transação duplicada detectada para chave {}", request.idempotencyKey());
            return existingTransaction.get();
        }

        var account = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada"));

        BigDecimal currentBalance = ledgerEntryRepository.calculateBalanceByAccountId(account.getId());
        if (currentBalance.compareTo(request.amount()) < 0) {
            log.warn("Saldo insuficiente para conta {}. Saldo atual: {}, Solicitado: {}", 
                    account.getId(), currentBalance, request.amount());
            throw new InsufficientBalanceException("Saldo insuficiente para realizar a transferência");
        }

        var transaction = TransactionEntity.builder()
                .idempotencyKey(request.idempotencyKey())
                .sourceAccountId(account.getId())
                .destinationPixKey(request.destinationPixKey())
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .build();
        
        transaction = transactionRepository.save(transaction);

        var debitEntry = LedgerEntryEntity.builder()
                .sourceAccountId(account.getId())
                .transactionId(transaction.getId())
                .entryType(EntryType.DEBIT)
                .amount(request.amount())
                .build();
        
        ledgerEntryRepository.save(debitEntry);

        transaction.setStatus(TransactionStatus.SETTLED);
        return transactionRepository.save(transaction);
    }
}
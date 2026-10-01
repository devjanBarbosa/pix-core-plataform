package com.pixcore.account_service;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.pixcore.domain.DTO.TransferRequest;
import com.pixcore.domain.entity.AccountEntity;
import com.pixcore.domain.entity.LedgerEntryEntity;
import com.pixcore.domain.entity.LedgerEntryEntity.EntryType;
import com.pixcore.domain.entity.TransactionEntity.TransactionStatus;
import com.pixcore.domain.exception.InsufficientBalanceException;
import com.pixcore.domain.repository.AccountRepository;
import com.pixcore.domain.repository.LedgerEntryRepository;
import com.pixcore.domain.repository.TransactionRepository;
import com.pixcore.domain.service.TransferService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest 
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private AccountEntity testAccount;

    @BeforeEach
    void setUp() {
        // Limpa a mesa antes de cada teste para um não atrapalhar o outro
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // 1. Cria uma conta bancária de teste
        testAccount = AccountEntity.builder()
                .accountNumber("123456-7")
                .build();
        testAccount = accountRepository.save(testAccount);

        // 2. Coloca um saldo inicial de R$ 100,00 no Ledger (CREDIT)
        var initialDeposit = LedgerEntryEntity.builder()
                .sourceAccountId(testAccount.getId())
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("100.00"))
                .build();
        ledgerEntryRepository.save(initialDeposit);
    }

    @Test
    @DisplayName("Deve realizar transferência com sucesso e atualizar o saldo no Ledger")
    void shouldExecuteTransferSuccessfully() {
        // Cenário: Transferir R$ 40,00
        var request = new TransferRequest(
                UUID.randomUUID().toString(),
                testAccount.getId(),
                "chave-pix-destino@banco.com",
                new BigDecimal("40.00")
        );

        // Execução
        var result = transferService.executeTransfer(request);

        // Validações
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SETTLED);

        // O saldo que era 100 deve ter virado exatamente 60
        BigDecimal finalBalance = ledgerEntryRepository.calculateBalanceByAccountId(testAccount.getId());
        assertThat(finalBalance).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Deve barrar transferência se o saldo for insuficiente")
    void shouldThrowExceptionWhenBalanceIsInsufficient() {
        // Cenário: Tentar transferir R$ 150,00 tendo apenas R$ 100,00
        var request = new TransferRequest(
                UUID.randomUUID().toString(),
                testAccount.getId(),
                "chave-pix-destino@banco.com",
                new BigDecimal("150.00")
        );

        // Execução e Validação do Erro
        assertThatThrownBy(() -> transferService.executeTransfer(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Saldo insuficiente");

        // O saldo deve continuar intacto em 100
        BigDecimal finalBalance = ledgerEntryRepository.calculateBalanceByAccountId(testAccount.getId());
        assertThat(finalBalance).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Deve garantir idempotência e não debitar duas vezes a mesma requisição")
    void shouldEnsureIdempotencyOnDuplicateRequests() {
        String sameKey = "minha-chave-de-idempotencia-unica";

        var request1 = new TransferRequest(
                sameKey,
                testAccount.getId(),
                "chave-pix-destino@banco.com",
                new BigDecimal("30.00")
        );

        var request2 = new TransferRequest(
                sameKey,
                testAccount.getId(),
                "chave-pix-destino@banco.com",
                new BigDecimal("30.00")
        );

        // Primeira execução
        var result1 = transferService.executeTransfer(request1);
        // Segunda execução (o cliente clicou duas vezes seguidas)
        var result2 = transferService.executeTransfer(request2);

        // Ambas devem devolver a mesma transação
        assertThat(result1.getId()).isEqualTo(result2.getId());

        // O saldo só pode ter sido debitado UMA vez (100 - 30 = 70)
        BigDecimal finalBalance = ledgerEntryRepository.calculateBalanceByAccountId(testAccount.getId());
        assertThat(finalBalance).isEqualByComparingTo("70.00");
    }
}
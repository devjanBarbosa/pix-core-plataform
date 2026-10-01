package com.pixcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pixcore.domain.entity.AccountEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    
    // O Spring lê o nome desse método e já cria o SELECT por conta própria:
    // "SELECT * FROM accounts WHERE account_number = ?"
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
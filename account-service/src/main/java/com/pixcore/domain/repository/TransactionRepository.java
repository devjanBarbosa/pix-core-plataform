package com.pixcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pixcore.domain.entity.TransactionEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
  
}

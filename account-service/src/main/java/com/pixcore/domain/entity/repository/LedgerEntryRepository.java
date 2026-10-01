package com.pixcore.domain.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pixcore.domain.entity.LedgerEntryEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE 
                    WHEN l.entryType = com.pixcore.account.domain.entity.LedgerEntryEntity$EntryType.CREDIT THEN l.amount 
                    ELSE -l.amount 
                END
            ), 
            0.00
        )
        FROM LedgerEntryEntity l
        WHERE l.sourceAccountId = :accountId
    """)
    BigDecimal calculateBalanceByAccountId(@Param("accountId") UUID accountId);
}
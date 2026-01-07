package com.example.logproducer.txshipper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionOutboxRepository extends JpaRepository<TransactionOutbox, Long> {
}

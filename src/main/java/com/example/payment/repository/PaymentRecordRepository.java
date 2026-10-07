package com.example.payment.repository;

import com.example.payment.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord,Long> {
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
}

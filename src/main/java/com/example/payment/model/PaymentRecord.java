package com.example.payment.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "payment_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_idempotency",
                columnNames = "idempotency_key"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private long amountMinor;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, length = 36)
    private String paymentId;

    @Column(nullable = false)
    private boolean approved;

    public PaymentRecord(
            String idempotencyKey,
            Long orderId,
            long amountMinor,
            String currency) {

        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.amountMinor = amountMinor;
        this.currency = currency;

        this.paymentId = UUID.randomUUID().toString();
        this.approved = true;
    }
}
package com.example.payment.service;

import com.example.payment.PaymentRequest;
import com.example.payment.PaymentResponse;
import com.example.payment.model.PaymentRecord;
import com.example.payment.repository.PaymentRecordRepository;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentRecordRepository repository;

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {

        Optional<PaymentResponse> existing =this.findExisting(request);

        if (existing.isPresent()) {
            return existing.get();
        }

        PaymentRecord record = new PaymentRecord(
                request.getIdempotencyKey(),
                request.getOrderId(),
                request.getAmountMinor(),
                request.getCurrency()
        );

        repository.saveAndFlush(record);

        return responseFor(record, request);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> findExisting(
            PaymentRequest request) {

        return repository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .map(record -> responseFor(record, request));
    }

    private PaymentResponse responseFor(
            PaymentRecord record,
            PaymentRequest request) {

        boolean sameRequest =
                record.getOrderId() == request.getOrderId()
                && record.getAmountMinor() == request.getAmountMinor()
                && record.getCurrency().equals(request.getCurrency());

        if (!sameRequest) {
            System.out.println("im hear i pass correct");
            throw Status.INVALID_ARGUMENT
                    .withDescription(
                            "Idempotency key was already used "
                                    + "with different payment information"
                    )
                    .asRuntimeException();
        }

        return PaymentResponse.newBuilder()
                .setPaymentId(record.getPaymentId())
                .setApproved(record.isApproved())
                .build();
    }
}
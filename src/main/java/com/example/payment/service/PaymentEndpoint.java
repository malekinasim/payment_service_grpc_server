package com.example.payment.service;

import com.example.payment.PaymentRequest;
import com.example.payment.PaymentResponse;
import com.example.payment.PaymentServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import io.grpc.Context;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEndpoint
        extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final PaymentProcessor processor;
    @Override
    public void pay(
            PaymentRequest request,
            StreamObserver<PaymentResponse> responseObserver) {

        String key = request.getIdempotencyKey();

        if (key.isBlank() || key.length() > 100) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Idempotency key must contain 1 to 100 characters"
                            )
                            .asRuntimeException()
            );
            return;
        }

        if (request.getAmountMinor() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    "amount_minor must be greater than zero"
                            )
                            .asRuntimeException()
            );
            return;
        }
        try {

            PaymentResponse response = payWithDuplicateRecovery(request);

// The database transaction has completed.
            System.out.println(
                    "Committed paymentId: " + response.getPaymentId()
            );

            if (Context.current().isCancelled()) {
                System.out.println(
                        "RPC cancelled; payment result is already stored"
                );
                return;
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException exception) {
            responseObserver.onError(exception);

        } catch (RuntimeException exception) {//for parallel same request
            log.error("Payment processing failed", exception);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Payment processing failed")
                            .asRuntimeException()
            );
        }
    }

    private PaymentResponse payWithDuplicateRecovery(
            PaymentRequest request) {

        try {
            return processor.pay(request);

        } catch (DataIntegrityViolationException exception) {

            return processor.findExisting(request)
                    .orElseThrow(() -> exception);
        }
    }
}
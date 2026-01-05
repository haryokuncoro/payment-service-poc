package com.example.demo.service;

import com.example.demo.model.IdemStatus;
import com.example.demo.model.Payment;
import com.example.demo.model.Wallet;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PaymentServiceV2 {

    private final WalletRepository walletRepo;
    private final PaymentRepository paymentRepo;
    private final StringRedisTemplate redis;

    private static final Duration IDEM_TTL = Duration.ofMinutes(10);

    public PaymentServiceV2(
            WalletRepository walletRepo,
            PaymentRepository paymentRepo,
            StringRedisTemplate redis
    ) {
        this.walletRepo = walletRepo;
        this.paymentRepo = paymentRepo;
        this.redis = redis;
    }

    public Payment pay(
            Long userId,
            String orderId,
            Long amount,
            String idemKey
    ) {

        String redisKey = "idem:" + idemKey;

        // 1. Check idempotency state
        IdemRecord record = getIdem(redisKey);

        if (record != null) {
            switch (record.status()) {
                case SUCCESS -> {
                    return paymentRepo.findById(record.paymentId())
                            .orElseThrow();
                }
                case IN_PROGRESS -> {
                    throw new IllegalStateException("Request is still processing");
                }
                case FAILED -> {
                    // allowed to retry
                }
            }
        }

        // 2. Mark IN_PROGRESS (atomic)
        boolean locked = Boolean.TRUE.equals(
                redis.opsForValue()
                        .setIfAbsent(redisKey, "IN_PROGRESS", IDEM_TTL)
        );

        if (!locked) {
            // race: someone else just set it
            throw new IllegalStateException("Request is still processing");
        }

        try {
            Payment payment = doPay(userId, orderId, amount);

            // 3. Mark SUCCESS
            redis.opsForValue().set(
                    redisKey,
                    "SUCCESS:" + payment.getId(),
                    IDEM_TTL
            );

            return payment;

        } catch (Exception e) {
            // 4. Mark FAILED so client retry is allowed
            redis.opsForValue().set(
                    redisKey,
                    "FAILED",
                    IDEM_TTL
            );
            throw e;
        }
    }

    @Transactional
    protected Payment doPay(Long userId, String orderId, Long amount) {

        // DB unique constraint safety
        paymentRepo.findByOrderId(orderId)
                .ifPresent(p -> {
                    throw new IllegalStateException("Order already paid");
                });

        // Optimistic locking here
        Wallet wallet = walletRepo.findById(userId)
                .orElseThrow();

        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepo.save(wallet);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");

        return paymentRepo.save(payment);
    }

    // ---------- helper ----------

    private IdemRecord getIdem(String key) {
        String raw = redis.opsForValue().get(key);
        if (raw == null) return null;

        if (raw.startsWith("SUCCESS")) {
            Long paymentId = Long.parseLong(raw.split(":")[1]);
            return new IdemRecord(IdemStatus.SUCCESS, paymentId);
        }
        if (raw.equals("IN_PROGRESS")) {
            return new IdemRecord(IdemStatus.IN_PROGRESS, null);
        }
        if (raw.equals("FAILED")) {
            return new IdemRecord(IdemStatus.FAILED, null);
        }
        return null;
    }

    private record IdemRecord(IdemStatus status, Long paymentId) {}
}

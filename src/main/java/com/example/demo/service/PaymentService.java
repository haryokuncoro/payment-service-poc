package com.example.demo.service;

import com.example.demo.infra.RedisLock;
import com.example.demo.model.Payment;
import com.example.demo.model.Wallet;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PaymentService {

    private final WalletRepository walletRepo;
    private final PaymentRepository paymentRepo;
    private final RedisLock redisLock;
    private final StringRedisTemplate redis;

    public PaymentService(
            WalletRepository walletRepo,
            PaymentRepository paymentRepo,
            RedisLock redisLock,
            StringRedisTemplate redis
    ) {
        this.walletRepo = walletRepo;
        this.paymentRepo = paymentRepo;
        this.redisLock = redisLock;
        this.redis = redis;
    }

    @Transactional
    public Payment pay(Long userId, String orderId, Long amount, String idemKey) {

        // 1. Idempotency
        String idemRedisKey = "idem:" + idemKey;
        if (Boolean.TRUE.equals(redis.hasKey(idemRedisKey))) {
            return paymentRepo.findByOrderId(orderId).orElseThrow();
        }

        // 2. Distributed lock
        String lockKey = "lock:order:" + orderId;
        if (!redisLock.lock(lockKey, Duration.ofSeconds(10))) {
            throw new IllegalStateException("Request in progress");
        }

        try {
            // 3. DB uniqueness
            paymentRepo.findByOrderId(orderId)
                    .ifPresent(p -> { throw new IllegalStateException("Already paid"); });

            // 4. Optimistic locking
            Wallet wallet = walletRepo.findById(userId).orElseThrow();

            if (wallet.getBalance() < amount) {
                throw new IllegalStateException("Insufficient balance");
            }

            wallet.setBalance(wallet.getBalance() - amount);
            walletRepo.save(wallet);

            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            payment.setStatus("SUCCESS");

            Payment saved = paymentRepo.save(payment);

            redis.opsForValue().set(idemRedisKey, "DONE", Duration.ofMinutes(10));

            return saved;

        } finally {
            redisLock.unlock(lockKey);
        }
    }
}

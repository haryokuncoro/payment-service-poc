## Happy Path Test
```
curl -X POST "http://localhost:8080/payments?userId=1&orderId=ORD-1&amount=10000" \
  -H "Idempotency-Key: key-1"
```
### Expected
- HTTP 200
- Payment created
- Wallet balance reduced


## Idempotency Test
```
curl -X POST "http://localhost:8080/payments?userId=1&orderId=ORD-1&amount=10000" \
  -H "Idempotency-Key: key-1"
```

### Expected
- No new payment row
- Same response returned
- Wallet balance unchanged

## Duplicate Order, Different Idempotency Key
```
curl -X POST "http://localhost:8080/payments?userId=1&orderId=ORD-1&amount=10000" \
  -H "Idempotency-Key: key-2"
```
### Expected
- Error: Already paid
- DB unique constraint saves you
- No second payment

## Concurrency Test
Terminal 1
```
curl -X POST "http://localhost:8080/payments?userId=1&orderId=ORD-101&amount=30000" \
  -H "Idempotency-Key: key-A" &

curl -X POST "http://localhost:8080/payments?userId=1&orderId=ORD-101&amount=30000" \
  -H "Idempotency-Key: key-B" &

```

### Expected
- One succeeds
- One fails with “Request in progress” or “Already paid”
- Only one payment row exists
- Wallet deducted once

## Optimistic Lock Test (Wallet Race)
- Manually lower balance:
```
UPDATE wallets SET balance = 20000 WHERE user_id = 1;
```
- Now try two payments at once each charging 15000.
### Expected:
- One succeeds
- One fails (optimistic lock or insufficient balance)
- Balance never goes negative
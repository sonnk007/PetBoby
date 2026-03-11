# Kafka – Quản lý topic, consumer, offset và log message

Tài liệu thực hành: cách quản lý topic, xem consumer/offset, và pattern log request/message trong PetBoby (và môi trường Kafka nói chung).

---

## 1. Quản lý topic

### 1.1. Topic là gì?

- **Topic**: Tên kênh (vd: `order.created`). Producer gửi message vào topic; consumer subscribe topic để đọc.
- **Partition**: Topic có thể có nhiều partition (mặc định 1). Message có **key** → Kafka hash key để chọn partition → message cùng key vào cùng partition (đảm bảo thứ tự theo key). Tăng partition → tăng throughput (nhiều consumer trong group đọc song song).
- **Replication factor**: Số bản copy partition (cluster). PetBoby dev thường 1 broker → replication = 1.

### 1.2. Tạo topic (command line)

Kafka đi kèm script `kafka-topics.sh` (trong thư mục bin của Kafka, hoặc trong container).

```bash
# Tạo topic "order.created", 2 partition, replication 1 (single broker)
kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic order.created --partitions 2 --replication-factor 1

# Nếu dùng Docker (container tên kafka):
docker exec -it kafka kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic order.created --partitions 2 --replication-factor 1
```

### 1.3. Liệt kê / mô tả topic

```bash
# Liệt kê tất cả topic
kafka-topics.sh --list --bootstrap-server localhost:9092

# Chi tiết topic (partition, replication, leader)
kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic order.created
```

### 1.4. Tự tạo topic từ Spring Boot (PetBoby)

Spring Kafka **không** tự tạo topic theo `application.yml` trừ khi bật **auto-create**. Có hai cách:

- **Cách 1 (dev)**: Bật auto-create trong config:
  ```yaml
  spring:
    kafka:
      admin:
        auto-create: true
  ```
  Lần đầu producer gửi vào topic chưa tồn tại → broker có thể tạo topic (phụ thuộc cấu hình broker `auto.create.topics.enable`).

- **Cách 2 (khuyến nghị)**: Tạo topic bằng script hoặc **KafkaAdmin** trong code. Trong PetBoby **đã có** bean `NewTopic` trong `order/config/KafkaProducerConfig`: topic `order.created`, 2 partition, 1 replica. Khi Order service khởi động, Spring Kafka sẽ tạo topic nếu chưa tồn tại (broker phải cho phép).

---

## 2. Quản lý consumer và consumer group

### 2.1. Consumer group là gì?

- **Consumer group**: Nhiều consumer instance cùng `group.id` **chia nhau** đọc message. Mỗi partition của topic được gán cho **một** consumer trong group. Thêm instance → rebalance (partition được gán lại).
- **Lag**: Số message chưa được consumer trong group xử lý (offset hiện tại của partition trừ offset đã commit). Lag cao → consumer chậm hoặc quá tải.

### 2.2. Liệt kê consumer group và offset

```bash
# Liệt kê tất cả consumer group
kafka-consumer-groups.sh --list --bootstrap-server localhost:9092

# Mô tả group "product-service": member, partition assigned, current offset, lag
kafka-consumer-groups.sh --describe --bootstrap-server localhost:9092 --group product-service
```

Output mẫu:
```
TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order.created   0          42             42              0
order.created   1          38             38              0
```

- **CURRENT-OFFSET**: Offset consumer đã commit (đã đọc đến đây).
- **LOG-END-OFFSET**: Offset mới nhất của partition (producer đã ghi đến đây).
- **LAG** = LOG-END-OFFSET - CURRENT-OFFSET. LAG > 0 → còn message chưa xử lý.

### 2.3. Reset offset (cẩn thận)

Khi cần “đọc lại” từ đầu hoặc từ một thời điểm (vd: sau khi sửa bug consumer):

```bash
# Reset offset group "product-service" cho topic "order.created" về đầu (earliest)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group product-service \
  --topic order.created --reset-offsets --to-earliest --execute

# Reset về cuối (bỏ qua message cũ)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group product-service \
  --topic order.created --reset-offsets --to-latest --execute
```

**Lưu ý**: Consumer trong group phải **dừng** khi reset offset (nếu không có thể bị rebalance và kết quả không như ý). Production nên có quy trình (dừng consumer → reset → start lại).

---

## 3. Offset là gì và cách commit

### 3.1. Offset là gì?

- **Offset**: Mỗi message trong partition có một **offset** (số thứ tự tăng dần). Consumer đọc theo thứ tự offset; sau khi xử lý xong (hoặc theo batch) sẽ **commit offset** để lần sau không đọc lại.
- **Commit**: “Tôi đã xử lý đến offset X”. Broker lưu offset theo consumer group. Restart consumer → đọc tiếp từ offset đã commit.

### 3.2. Commit strategy (Spring Kafka)

- **enable.auto.commit = true (mặc định)**: Consumer tự commit offset theo chu kỳ (vd: 5 giây). **Rủi ro**: Đã commit nhưng chưa xử lý xong → crash → message bị bỏ qua. Hoặc xử lý xong chưa kịp commit → crash → đọc lại (duplicate).
- **enable.auto.commit = false**: Commit bằng tay sau khi xử lý xong (trong listener). **At-least-once**: Xử lý xong rồi commit → không mất message; nếu crash trước khi commit → đọc lại (cần idempotent).

Trong PetBoby (Spring): mặc định dùng auto commit. Khi cần “xử lý xong mới commit” có thể dùng `AckMode.MANUAL_IMMEDIATE` hoặc `MANUAL` và gọi `ack.acknowledge()` trong listener (khi dùng `ConsumerRecord` + `Acknowledgment`).

### 3.3. Xem offset trong log (PetBoby)

Trong listener, nhận thêm `ConsumerRecord` (hoặc `@Header`) để lấy partition, offset, timestamp và **log** ra (xem phần 5 bên dưới).

---

## 4. Log request / message – mục đích và cách làm

### 4.1. Tại sao cần log?

- **Producer**: Log **trước/sau khi gửi** (topic, key, payload hoặc summary) → trace “order X đã gửi event”, debug lỗi gửi.
- **Consumer**: Log **khi nhận** (topic, partition, offset, key, payload hoặc summary) → trace “đã nhận message từ partition X offset Y”, debug duplicate hoặc thứ tự, audit.

### 4.2. Log gì (vừa đủ, tránh lộ dữ liệu)?

- **Nên**: topic, partition, offset, key, timestamp; với payload có thể log **summary** (vd: orderId, orderCode) hoặc size. Trong production tránh log full payload nếu có dữ liệu nhạy cảm.
- **Có thể**: full payload khi debug (dev). Cấu hình log level (DEBUG = log chi tiết, INFO = log summary).

### 4.3. Pattern trong code (PetBoby)

- **Producer** (KafkaOrderEventPublisher): Log **trước** khi send (topic, key, orderId, orderCode) và trong callback **sau** khi send (thành công: topic, partition, offset; lỗi: exception).
- **Consumer** (OrderCreatedKafkaListener): Nhận `ConsumerRecord<String, OrderCreatedPayload>` để có partition, offset; log topic, partition, offset, key, và payload summary (orderId, orderCode, totalAmount…).

Chi tiết code: xem phần “Ứng dụng trong PetBoby” trong file `PetBoby-backend-knowledge-to-code.md` mục 7.5 và code trong `KafkaOrderEventPublisher`, `OrderCreatedKafkaListener`.

---

## 5. Tóm tắt lệnh nhanh

| Mục đích | Lệnh |
|----------|------|
| Tạo topic | `kafka-topics.sh --create --bootstrap-server localhost:9092 --topic order.created --partitions 2 --replication-factor 1` |
| List topic | `kafka-topics.sh --list --bootstrap-server localhost:9092` |
| Describe topic | `kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic order.created` |
| List consumer group | `kafka-consumer-groups.sh --list --bootstrap-server localhost:9092` |
| Describe group (offset, lag) | `kafka-consumer-groups.sh --describe --bootstrap-server localhost:9092 --group product-service` |
| Reset offset (earliest) | `kafka-consumer-groups.sh ... --group product-service --topic order.created --reset-offsets --to-earliest --execute` |

---

## 6. So sánh Saga và Event-Driven trong PetBoby

### 6.1. Khái niệm nhanh

- **Event-Driven Architecture (EDA)**:
  - Service phát event (vd: `order.created`) lên broker.
  - Service khác subscribe và xử lý độc lập.
  - Mục tiêu chính: **decouple** và mở rộng dễ.

- **Saga**:
  - Là pattern xử lý **giao dịch phân tán** qua nhiều service.
  - Mỗi bước là local transaction + event.
  - Nếu lỗi ở bước sau thì chạy **compensation** để hoàn tác nghiệp vụ đã làm ở bước trước.
  - Saga thường triển khai bằng event-driven (choreography) hoặc điều phối trung tâm (orchestration).

### 6.2. Saga vs Event-Driven (so sánh trực tiếp)

| Tiêu chí | Event-Driven (chung) | Saga |
|---|---|---|
| Mục tiêu | Truyền thông điệp, tách service | Đảm bảo tính nhất quán nghiệp vụ nhiều bước |
| Tính bắt buộc rollback | Không bắt buộc | Có cơ chế compensation khi fail |
| Độ phức tạp | Trung bình | Cao hơn rõ rệt |
| Theo dõi state giao dịch | Thường không có state machine rõ | Cần state saga (PENDING/SUCCESS/FAILED/COMPENSATED) |
| Khi phù hợp | Notification, analytics, audit, đồng bộ phụ | Payment, inventory reserve, loyalty point, refund |

### 6.3. Ưu/nhược điểm khi áp dụng vào PetBoby hiện tại

#### A) Event-Driven (cách PetBoby đang dùng)

- **Ưu điểm**:
  - Nhanh để triển khai: Order publish `order.created`, Product consume để log/xử lý phụ.
  - Dễ scale: thêm consumer mới (notification, loyalty, analytics) mà không sửa `order` nhiều.
  - Giảm coupling runtime: order không cần chờ service downstream hoàn tất.

- **Nhược điểm**:
  - Chỉ dừng ở mức “thông báo sự kiện”, chưa giải quyết trọn vẹn giao dịch nhiều bước.
  - Dễ phát sinh eventual consistency và duplicate nếu consumer chưa idempotent đầy đủ.
  - Khi thêm payment/inventory thật sẽ khó kiểm soát trạng thái tổng thể nếu không có saga/state rõ.

#### B) Saga (mức nên hướng tới khi nghiệp vụ tài chính tăng)

- **Ưu điểm**:
  - Quản lý được luồng nghiệp vụ dài: `CreateOrder -> ReserveInventory -> ChargePayment -> ConfirmOrder`.
  - Có **compensation** khi lỗi: ví dụ payment fail thì release inventory và đánh dấu order fail.
  - Phù hợp các luồng nhạy cảm tiền/tồn kho cần audit trạng thái từng bước.

- **Nhược điểm**:
  - Chi phí thiết kế/vận hành cao hơn: event contract, state machine, timeout, retry, idempotency, DLQ.
  - Debug khó hơn event-driven cơ bản vì nhiều trạng thái trung gian.
  - Cần kỷ luật kỹ thuật cao: correlation id, event id, processed-events, monitoring lag/retry.

### 6.4. Khuyến nghị thực tế cho PetBoby

- **Hiện tại** (đang đúng): giữ Event-Driven đơn giản cho `order.created` để học nền tảng Kafka.
- **Giai đoạn kế tiếp**:
  1. Thêm idempotent consumer chuẩn (`processed_events` theo `eventId`).
  2. Thêm outbox cho publish event từ `order`.
  3. Khi có payment/inventory thật, nâng cấp lên **Saga choreography skeleton**:
     - Event gợi ý: `order.created`, `inventory.reserved|failed`, `payment.completed|failed`, `order.confirmed|cancelled`.
     - Có trạng thái nghiệp vụ ở order (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, ...).
     - Có compensation rõ khi fail.

### 6.5. Code skeleton đã thêm trong PetBoby (để thực hành ngay)

- **Event contracts**: `order/src/main/java/com/sonnk/order/application/event/saga/*`
- **Saga state machine (order)**:
  - `order/src/main/java/com/sonnk/order/model/entity/enums/OrderSagaState.java`
  - `order/src/main/java/com/sonnk/order/infrastructure/messaging/OrderSagaCoordinatorListener.java`
- **Inventory choreography (product)**:
  - `product/src/main/java/com/sonnk/product/infrastructure/messaging/SagaInventoryChoreographyListener.java`
  - `product/src/main/java/com/sonnk/product/infrastructure/messaging/SagaInventoryEventPublisher.java`
- **Payment step demo (order)**:
  - `order/src/main/java/com/sonnk/order/infrastructure/messaging/SagaPaymentProcessorListener.java`
- **Topic constants + topic creation**:
  - `order/src/main/java/com/sonnk/order/infrastructure/messaging/SagaTopics.java`
  - `order/src/main/java/com/sonnk/order/config/KafkaProducerConfig.java`

Mẹo test nhanh:
- Tạo order với `branchCode` chứa `FAIL-INV` → inventory fail → order bị cancel.
- Tạo order với `branchCode` chứa `FAIL-PAY` → payment fail → phát compensation `saga.inventory.release.requested`.
- Trường hợp còn lại → flow success, order đi tới trạng thái `PAID` và saga `COMPLETED`.

### 6.6. Idempotent consumer – `processed_events` table

Bổ sung đã có trong code (production-grade idempotency):

#### Cơ chế hoạt động

```
Consumer nhận message
    │
    ▼
tryMarkProcessed(eventId, consumerGroup, topic)
    ├─ existsByEventIdAndConsumerGroup? → true  → LOG warn + return (skip)
    └─ false → INSERT processed_events (cùng transaction)
                    │
                    ▼
              Business logic
                    │
                    ▼
              COMMIT (processed_events + business changes đồng thời)
              hoặc ROLLBACK cả hai nếu business fail → retry hợp lệ
```

- **Cùng transaction**: `IdempotentConsumerHelper` không dùng `REQUIRES_NEW`. Nếu dùng `REQUIRES_NEW`, processed_events commit trước; nếu business sau đó fail → message bị bỏ qua mãi → mất event.
- **Unique constraint** `(event_id, consumer_group)`: phòng tuyến cuối chặn race condition khi nhiều instance cùng xử lý message. Instance thua INSERT bắt `DataIntegrityViolationException` → skip an toàn.

#### File code đã thêm

| File | Module | Mục đích |
|---|---|---|
| `model/entity/ProcessedEvent.java` | order, product | Entity + unique constraint |
| `repository/ProcessedEventRepository.java` | order, product | `existsByEventIdAndConsumerGroup`, `findByProcessedAtBefore` |
| `infrastructure/idempotency/IdempotentConsumerHelper.java` | order, product | `tryMarkProcessed()` helper |

#### Cleanup job (cần làm cho production)

```java
// Xoá row cũ hơn 7 ngày (tránh bảng phình lớn theo thời gian)
@Scheduled(cron = "0 0 3 * * *")
@Transactional
public void cleanupOldProcessedEvents() {
    List<ProcessedEvent> old = processedEventRepository
        .findByProcessedAtBefore(LocalDateTime.now().minusDays(7));
    processedEventRepository.deleteAll(old);
}
```

> Tóm lại: **Event-Driven** là nền tảng giao tiếp bất đồng bộ; **Saga** là lớp chiến lược phía trên để đảm bảo nhất quán giao dịch nhiều bước. Với PetBoby hiện tại: dùng EDA là hợp lý, và nên tiến dần sang Saga khi đi vào payment/inventory production.

---

**Đọc thêm**:
- **PetBoby-backend-knowledge-to-code.md** mục **7.5** (topic/consumer/offset, log message).
- **kafka-data-integrity-and-deep-dive.md** (tính toàn vẹn dữ liệu giao dịch/thanh toán, cơ chế sâu Kafka, ứng dụng thực tế).



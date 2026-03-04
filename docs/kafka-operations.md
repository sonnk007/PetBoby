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

**Đọc thêm**:
- **PetBoby-backend-knowledge-to-code.md** mục **7.5** (topic/consumer/offset, log message).
- **PetBoby/docs/kafka-data-integrity-and-deep-dive.md** (tính toàn vẹn dữ liệu giao dịch/thanh toán, cơ chế sâu Kafka, ứng dụng thực tế).

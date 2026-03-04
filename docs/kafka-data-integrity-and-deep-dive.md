# Kafka – Tính toàn vẹn dữ liệu (giao dịch/thanh toán), cơ chế vận hành sâu và ứng dụng thực tế

Tài liệu bổ sung cho **PetBoby-backend-knowledge-to-code.md** mục 7: cách đảm bảo tính toàn vẹn dữ liệu khi dùng Kafka cho giao dịch/thanh toán, cơ chế bên trong Kafka, và cách tận dụng Kafka trong hệ thống thực tế.

---

## PHẦN A. ĐẢM BẢO TÍNH TOÀN VẸN DỮ LIỆU (GIAO DỊCH, THANH TOÁN)

### A.1. Vấn đề cốt lõi

Khi dùng Kafka trong luồng **giao dịch** (đặt hàng, thanh toán, trừ tồn kho):

- **Eventual consistency**: Order service lưu đơn xong, gửi event; Payment service nhận sau vài trăm ms. Nếu đọc “số dư” ngay sau khi order tạo → có thể chưa cập nhật.
- **Duplicate message (at-least-once)**: Consumer có thể nhận **cùng một message nhiều lần** (retry, restart, rebalance). Nếu mỗi lần đều “trừ tiền” hoặc “trừ tồn kho” → **sai dữ liệu**.
- **Thứ tự**: Message cùng key (vd: orderId) cần xử lý **đúng thứ tự** (OrderCreated → OrderPaid → OrderShipped). Partition đảm bảo thứ tự trong một partition; nhiều partition cho cùng key thì phải dùng **partition key = business key**.
- **Mất message**: Producer gửi xong nhưng chưa được broker ghi; hoặc consumer xử lý xong chưa commit offset thì crash → có thể mất hoặc trùng tùy cấu hình.

Mục tiêu: **không trừ tiền/tồn kho hai lần**, **không mất event quan trọng**, **thứ tự đúng** với nghiệp vụ nhạy cảm (thanh toán, inventory).

---

### A.2. Giải pháp 1: Idempotency (idempotent consumer)

**Ý tưởng**: Mỗi message có **idempotency key** (vd: `orderId` hoặc `eventId`). Consumer trước khi xử lý **kiểm tra**: key này đã xử lý chưa? Nếu rồi → bỏ qua (hoặc trả success). Nếu chưa → xử lý, rồi **lưu** key đã xử lý.

**Cách làm**:

- Trong payload event: có field `eventId` (UUID) hoặc dùng `orderId` + `eventType` làm key.
- Consumer có bảng **processed_events** (eventId, processedAt). Trước khi trừ tồn kho / cập nhật thanh toán:
  - `SELECT 1 FROM processed_events WHERE event_id = ?` (hoặc `order_id = ? AND event_type = ?`).
  - Nếu có → skip (idempotent: xử lý lần 2 = không làm gì).
  - Nếu không → thực hiện nghiệp vụ (trừ tồn kho, ghi payment), rồi `INSERT INTO processed_events (event_id, ...)`.
- Gói trong **cùng transaction DB**: xử lý nghiệp vụ + insert processed_events trong một `@Transactional` → hoặc xử lý xong rồi commit offset (manual commit) sau khi commit DB.

**Áp dụng**: Thanh toán (payment service nhận event “order cần thanh toán” – xử lý một lần cho mỗi orderId); trừ tồn kho (inventory service: mỗi orderId chỉ trừ một lần).

**Lợi ích**: Đơn giản, áp dụng được với at-least-once delivery.  
**Hạn chế**: Cần lưu trữ key đã xử lý (DB hoặc cache với TTL đủ dài); key phải unique và nhất quán (eventId do producer sinh).

---

### A.3. Giải pháp 2: Transactional Outbox

**Vấn đề**: Service cần **vừa ghi DB vừa gửi event**. Nếu ghi DB xong rồi gửi Kafka mà crash giữa chừng → DB đã commit, event chưa gửi → **mất event**. Nếu gửi Kafka trước rồi ghi DB mà DB fail → event đã gửi nhưng nghiệp vụ chưa xong → **inconsistent**.

**Ý tưởng (Outbox)**:

1. Trong **cùng transaction DB** với nghiệp vụ (vd: insert Order): ghi thêm một bản ghi vào bảng **outbox** (id, aggregate_id, event_type, payload, created_at, status = PENDING).
2. Commit transaction → Order và outbox row cùng tồn tại hoặc cùng rollback.
3. Một **process riêng** (scheduled job hoặc CDC như Debezium) **đọc bảng outbox** (status = PENDING), gửi từng bản ghi lên Kafka, rồi update status = SENT (hoặc xóa). Nếu gửi Kafka fail → retry; không bao giờ xóa/update thành SENT cho đến khi Kafka acknowledge.

Kết quả: **Mỗi event tương ứng đúng một giao dịch DB**. Không mất event (vì outbox nằm trong DB); không gửi event “ảo” (vì chỉ gửi sau khi DB commit).

**Áp dụng**: Order service: khi tạo Order, ghi Order + outbox row “OrderCreated”; outbox publisher đọc và publish lên topic `order.created`. Payment service tương tự: ghi Payment + outbox “PaymentCompleted”.

**Lợi ích**: Đảm bảo “ghi DB thành công” ↔ “event sẽ được gửi” (ít nhất một lần).  
**Hạn chế**: Thêm bảng outbox, thêm process publisher; cần xử lý duplicate khi consumer at-least-once (idempotency như A.2).

---

### A.4. Giải pháp 3: Saga (giao dịch phân tán)

**Vấn đề**: Một nghiệp vụ nghiệp vụ trải nhiều service (Order → Payment → Inventory → Shipping). Không có distributed transaction 2PC trên Kafka.

**Ý tưởng (Saga)**:

- Mỗi service thực hiện **local transaction** và publish **event** (thành công hoặc thất bại).
- Service tiếp theo subscribe event, thực hiện bước của mình; nếu fail → publish **compensating event** (hoàn tiền, hoàn tồn kho…). Các service khác subscribe compensating event để “rollback” bước mình đã làm.

**Ví dụ**: OrderCreated → Payment service trừ tiền → PaymentCompleted. Nếu Inventory không đủ → publish PaymentRefund; Payment service subscribe và hoàn tiền.

**Áp dụng**: Luồng đặt hàng – thanh toán – tồn kho – vận chuyển; mỗi bước là event trên Kafka, failure xử lý bằng compensating event.

**Lợi ích**: Không cần 2PC; scale được.  
**Hạn chế**: Thiết kế phức tạp; có giai đoạn “tạm thời inconsistent” (vd đã trừ tiền chưa trừ tồn kho); cần idempotency cho mỗi bước và compensating logic rõ ràng.

---

### A.5. Giải pháp 4: Exactly-once semantics (Kafka transactions)

**Cơ chế Kafka**:

- **Idempotent producer**: Producer gán mỗi batch một **Producer ID** + **sequence number** theo partition. Broker ghi nhận; nếu nhận lại cùng PID + sequence (retry) thì **bỏ qua duplicate** → mỗi message chỉ ghi một lần.
- **Transactional producer**: Producer gọi `beginTransaction()` → send nhiều message → `commitTransaction()`. Message chỉ **visible** cho consumer khi commit. Consumer dùng `isolation.level=read_committed` → chỉ đọc message đã commit.
- **Consumer exactly-once**: Vừa xử lý vừa commit offset trong **cùng transaction** (Kafka transaction). Spring Kafka hỗ trợ `KafkaTransactionManager`: consume → xử lý (vd ghi DB) → commit offset trong transaction → nếu fail thì rollback (offset không commit, message đọc lại).

**Áp dụng**: Khi consumer **chỉ** cập nhật state trong Kafka (vd KStream) hoặc kết hợp với DB qua transactional outbox + read_committed. Với “consumer ghi DB + commit offset” thì cần Kafka transaction manager hoặc pattern “ghi DB + idempotency” rồi commit offset (at-least-once + idempotent = hiệu quả exactly-once).

**Lợi ích**: Giảm duplicate ở producer; consumer có thể đọc chỉ message đã commit.  
**Hạn chế**: Cấu hình và vận hành phức tạp hơn; một số client/version cần bật đúng config (enable.idempotence, transactional.id).

---

### A.6. Đảm bảo thứ tự (ordering)

- **Partition key = business key**: Gửi message với **key = orderId** (hoặc paymentId). Message cùng orderId vào **cùng partition** → consumer của partition đó xử lý **đúng thứ tự** theo offset.
- **Một partition cho mỗi entity quan trọng**: Nếu cần thứ tự toàn cục cho một orderId, đảm bảo key nhất quán. Nhiều partition vẫn đảm bảo thứ tự **trong từng key**.
- **Consumer**: Xử lý tuần tự trong một partition (single thread per partition) hoặc đảm bảo xử lý theo offset khi cần strict order.

**Áp dụng PetBoby**: OrderCreatedEvent đã dùng key = orderId → message cùng order vào cùng partition → Product service xử lý theo thứ tự.

---

### A.7. Tóm tắt pattern cho giao dịch / thanh toán

| Mục tiêu | Giải pháp | Ghi chú |
|----------|-----------|--------|
| Không xử lý trùng (duplicate) | **Idempotent consumer** (bảng processed_events / eventId) | Bắt buộc khi at-least-once |
| Không mất event khi ghi DB + gửi Kafka | **Transactional Outbox** (ghi DB + outbox trong 1 transaction; process đọc outbox gửi Kafka) | Chuẩn production cho “DB + event” |
| Giao dịch nhiều service | **Saga** (local tx + event; compensating event khi lỗi) | Thiết kế từng bước + compensation |
| Giảm duplicate producer + message commit | **Idempotent producer** + **Transactional producer** + consumer **read_committed** | Kafka native; kết hợp outbox/idempotency |
| Thứ tự theo orderId / paymentId | **Partition key = orderId** (hoặc entity id) | Đã áp dụng trong PetBoby |

---

## PHẦN B. CƠ CHẾ VẬN HÀNH SÂU CỦA KAFKA

### B.1. Lưu trữ (log, segment)

- **Log**: Mỗi partition là một **log** (append-only). Message ghi tuần tự; mỗi message có **offset** tăng dần trong partition.
- **Segment**: Log được chia **segment file** (theo size hoặc thời gian). Segment cũ có thể **retention** (xóa hoặc compact) để giải phóng disk. Đọc message = tìm segment + offset.
- **Index**: Segment có index (offset → vị trí file) để tìm nhanh message theo offset.

**Ứng dụng**: Hiểu retention (log.retention.hours / bytes) để không mất dữ liệu quá sớm; compacted topic cho state (chỉ giữ bản ghi mới nhất theo key).

---

### B.2. Replication (leader, ISR, acks)

- **Leader / Follower**: Mỗi partition có một **leader** (broker xử lý read/write) và **replicas** (follower) copy dữ liệu từ leader.
- **ISR (In-Sync Replicas)**: Tập replica đã “bắt kịp” leader (trong giới hạn lag). Khi ghi message, leader ghi và đợi replica trong ISR copy xong (tùy acks).
- **acks (producer)**:
  - **acks=0**: Không đợi; có thể mất message nếu leader chưa ghi xong đã crash.
  - **acks=1**: Đợi leader ghi xong; replica chưa kịp copy mà leader chết → có thể mất.
  - **acks=all** (hoặc -1): Đợi **tất cả replica trong ISR** ghi xong. Mất message chỉ khi toàn bộ ISR mất (rất hiếm).

**Ứng dụng**: Production nên **acks=all** cho topic giao dịch/thanh toán; replication.factor >= 2. PetBoby dev 1 broker thì acks=1 cũng chấp nhận.

---

### B.3. Producer: retry, idempotence, batching

- **Retry**: Producer mặc định retry khi gửi lỗi (network, timeout). Retry có thể gây **duplicate** nếu broker đã ghi lần đầu nhưng response bị mất. → Dùng **idempotent producer** (enable.idempotence=true) để broker loại duplicate theo PID + sequence.
- **Batching**: Producer gom message theo thời gian (linger.ms) hoặc size (batch.size) rồi gửi một lần → giảm số request, tăng throughput.
- **Compression**: Có thể nén batch (snappy, lz4, zstd) → giảm băng thông và dung lượng lưu trữ.

**Ứng dụng**: Bật idempotence cho producer giao dịch; điều chỉnh batch/linger cho throughput; compression khi payload lớn.

---

### B.4. Consumer: offset commit, rebalance

- **Offset commit**: Consumer đọc message, xử lý, rồi **commit offset** (theo chu kỳ auto hoặc manual sau khi xử lý xong). Commit = “tôi đã xử lý đến offset này”. Restart → đọc tiếp từ offset đã commit.
- **Auto commit**: Theo thời gian (auto.commit.interval.ms). Rủi ro: đã commit nhưng chưa xử lý xong → crash → mất message; hoặc xử lý xong chưa commit → crash → duplicate.
- **Manual commit**: Consumer gọi commit sau khi xử lý xong (và có thể trong cùng transaction với DB nếu dùng KafkaTransactionManager). At-least-once + idempotent consumer = hiệu quả exactly-once.
- **Rebalance**: Khi consumer join/leave group, partition được **assign lại** cho từng member. Trong lúc rebalance, consumer có thể commit offset và ngừng đọc; sau rebalance đọc tiếp từ offset mới (có thể duplicate một số message gần biên → lại cần idempotency).

**Ứng dụng**: Giao dịch/thanh toán nên **manual commit** sau khi xử lý + ghi DB (và dùng idempotency). Monitor rebalance để tránh “rebalance liên tục” (consumer chết, timeout session quá ngắn).

---

### B.5. Exactly-once (transactional producer + read_committed)

- **Producer**: `transactional.id` set, gọi `beginTransaction()` → send → `commitTransaction()`. Message chỉ được coi là “committed” khi commitTransaction. Broker lưu transaction state.
- **Consumer**: `isolation.level=read_committed` → chỉ đọc message thuộc transaction đã commit. Tránh đọc “half-written” transaction.
- **Kafka Streams / Kafka consumer với transaction**: Có thể commit offset trong transaction Kafka (KafkaTransactionManager) để “consume + process + commit offset” nguyên khối.

**Ứng dụng**: Khi cần exactly-once end-to-end trong Kafka (Streams); hoặc kết hợp với outbox + idempotent consumer cho DB + Kafka.

---

## PHẦN C. ỨNG DỤNG THỰC TẾ “HẾT CÔNG SUẤT”

### C.1. Luồng thanh toán / giao dịch (PetBoby-style)

1. **Order service**: Tạo Order trong DB; ghi **outbox** “OrderCreated” trong cùng transaction. Process outbox publisher gửi lên topic `order.created` (idempotent producer, acks=all). Message **key = orderId**.
2. **Payment service** (consumer): Subscribe `order.created` (hoặc topic riêng `order.payment.requested`). Với mỗi message, kiểm tra **processed_events(orderId)** → nếu đã có thì skip. Nếu chưa: tạo Payment, ghi outbox “PaymentCompleted”, commit offset (manual) sau khi commit DB. Idempotent producer khi gửi PaymentCompleted.
3. **Inventory service**: Tương tự: idempotent consumer (orderId), trừ tồn kho một lần, ghi outbox nếu cần, commit offset.
4. **Saga**: Nếu Inventory fail → publish “PaymentRefund”; Payment service subscribe và hoàn tiền (cũng idempotent theo refundId/orderId).

### C.2. Throughput cao

- **Partition**: Số partition >= số consumer trong group (để mỗi consumer bận). Tăng partition khi scale consumer (lưu ý: tăng partition sau có thể ảnh hưởng ordering với key mới).
- **Batching + compression**: Producer batch size, linger.ms; compression (lz4/zstd). Consumer fetch.min.bytes để giảm round-trip.
- **Consumer**: Nhiều instance cùng group; xử lý nhanh (async, không block); monitor lag.

### C.3. Monitoring và vận hành

- **Lag**: Lag = 0 hoặc ổn định; lag tăng đột biến = consumer chậm hoặc chết. Cảnh báo khi lag > ngưỡng.
- **Dead Letter Queue (DLQ)**: Message xử lý lỗi (deserialize fail, business exception sau N lần retry) gửi sang topic DLQ; không block partition; xử lý DLQ sau (manual hoặc batch fix).
- **Schema registry**: Dùng Avro/Protobuf + schema registry để version payload, tránh breaking change khi đổi schema event.

### C.4. Áp dụng trong PetBoby (gợi ý mở rộng)

- **OrderCreated**: Đã có; có thể thêm **eventId** (UUID) vào payload để consumer dùng làm idempotency key. Partition key = orderId (đã có).
- **Payment / Inventory**: Khi thêm service thanh toán, tồn kho: consumer dùng bảng **processed_events(orderId, eventId)**; manual commit sau khi ghi DB; outbox nếu service đó cũng publish event.
- **Config producer**: `acks=all`, `enable.idempotence=true`; topic replication >= 2 khi chạy cluster.

---

## Tóm tắt

- **Tính toàn vẹn**: Idempotent consumer (bắt buộc) + Transactional Outbox (ghi DB + event) + Saga (nhiều bước, compensating) + Exactly-once Kafka (khi cần) + partition key = business key (thứ tự).
- **Cơ chế sâu**: Log/segment, replication/ISR/acks, producer idempotence/batch, consumer commit/rebalance, transactional read_committed.
- **Thực tế**: Outbox + idempotency cho order/payment; acks=all, replication; monitor lag; DLQ; schema registry khi scale.

Đọc kèm **PetBoby-backend-knowledge-to-code.md** mục **7.6** và **kafka-operations.md** cho thao tác topic/consumer/offset.

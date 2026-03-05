## DOMAIN: HỆ THỐNG QUẢN LÝ CHUỖI CÀ PHÊ / TRÀ SỮA – `PetBoby`

File này mô tả **nghiệp vụ cốt lõi** mà `PetBoby` hướng tới, để dẫn dắt thiết kế backend (Java / Spring / microservices) và frontend sau này.

---

## 1. Bối cảnh & mục tiêu

- `PetBoby` là hệ thống quản lý cho **chuỗi quán cà phê / trà sữa** (nhiều chi nhánh).
- Người dùng chính:
  - **Khách hàng**: đặt đồ uống, tích điểm, xem lịch sử đơn hàng.
  - **Nhân viên quán (barista, thu ngân)**: tạo order tại quầy, nhận đơn online, xử lý thanh toán.
  - **Quản lý chi nhánh**: quản lý menu, giá, ca làm, báo cáo doanh thu chi nhánh.
  - **Admin hệ thống**: quản lý toàn bộ chuỗi, cấu hình chung (promotion global, cấu hình điểm, phân quyền, tài chính & nhân sự).
- Mục tiêu kỹ thuật:
  - Hệ thống phải dễ mở rộng (nhiều chi nhánh, nhiều loại đồ uống, nhiều kênh bán hàng).
  - Thiết kế hướng microservices: `user`, `product`, `order`, `gateway-service` hiện có là nền tảng.

---

## 2. Các domain chính

### 2.1. Product / Menu (module `product`)

Đây là nơi quản lý:

- **Category**: nhóm sản phẩm (COFFEE, TEA, MILK_TEA, TOPPING, FOOD, PROMO_DRINK, v.v.).
  - Thuộc tính chính:
    - `code`, `name`, trạng thái (active/inactive), metadata (mô tả, thứ tự hiển thị).

- **Product**: đồ uống / món ăn bán ra.
  - Ví dụ: “Trà sữa trân châu đường đen”, “Latte đá”, “Bạc xỉu”.
  - Thuộc tính nghiệp vụ mong muốn:
    - `name`, `description`, `category`, `basePrice`.
    - `productSize`: S / M / L / XL / XXL.
    - `status`: DRAFT (chưa bán), ACTIVE (đang bán), INACTIVE (ngừng bán).
    - `hasTopping`: có cho phép chọn topping hay không.
    - `imageUrl`: ảnh minh hoạ.
    - `toppings`: danh sách topping khả dụng với product (hiện đang lưu JSON list `ToppingInfo` trong entity).

- **Topping**: thêm vào đồ uống.
  - Ví dụ: trân châu đen, trân châu trắng, flan, thạch, cheese foam.
  - Thuộc tính:
    - `name`, `price`, `status` (ACTIVE/INACTIVE).

**Use cases chính (product/menu):**

1. Admin tạo / sửa / xoá mềm **Category**, **Product**, **Topping**.
2. Quản lý chi nhánh có thể:
   - Ẩn một số product ở chi nhánh riêng (sau này mở rộng).
   - Cập nhật giá chi nhánh (nếu cần phân biệt).
3. Frontend / POS cần:
   - API để **lấy menu hiển thị cho khách**:
     - Theo category, theo status ACTIVE, theo size.
   - API để lấy chi tiết product + danh sách topping khả dụng.

### 2.2. Order (module `order`) – sẽ phát triển tiếp

Quản lý toàn bộ vòng đời đơn hàng:

- Đặt hàng:
  - Order tại quầy (POS).
  - Order online (sau này).
- Cấu trúc đơn hàng cơ bản:
  - `Order`: id, mã đơn, khách hàng (optional), chi nhánh, tổng tiền, trạng thái (NEW, IN_PROGRESS, DONE, CANCELLED, PAID...).
  - `OrderItem`: tham chiếu `Product`, size, đơn giá, số lượng.
  - `OrderItemTopping`: topping cho từng item, phụ phí topping.
- Use cases:
  - Tạo đơn hàng mới từ danh sách product + toppings.
  - Tính tổng tiền, áp dụng promotion (sau này).
  - Theo dõi trạng thái đơn (barista nhận / đang pha / hoàn thành / đã thanh toán).

### 2.3. User / Customer / Employee (module `user`) – sẽ hoàn thiện lại

Hiện tại model còn rất sơ khai; target nghiệp vụ:

- **User** (đăng nhập hệ thống):
  - Có thể là **customer** (khách hàng có tài khoản) hoặc **employee** (nhân viên, quản lý, admin).
  - Thuộc tính chính: username, password (hash), roles, trạng thái.

- **Customer**:
  - Thông tin profile: tên, số điện thoại, email, lịch sử tích điểm.

- **Employee**:
  - Nhân viên chi nhánh: mã nhân viên, vai trò (cashier, barista, manager).

- **Profile**:
  - Thông tin cá nhân chi tiết (dùng chung cho cả customer & employee nếu cần).

Use cases:

- Đăng nhập / phân quyền.
- Gán quyền theo vai trò: admin chuỗi, quản lý chi nhánh, nhân viên, khách hàng.

---

## 3. Chức năng lớn của hệ thống cần hướng tới

1. **Quản lý menu chuỗi quán**:
   - CRUD category/product/topping.
   - Phân loại menu theo category.
   - Quản lý trạng thái (DRAFT/ACTIVE/INACTIVE).

2. **Tạo & xử lý đơn hàng**:
   - Chọn sản phẩm + size + toppings.
   - Tính tổng tiền.
   - Cập nhật trạng thái đơn hàng.

3. **Quản lý người dùng & phân quyền**:
   - Customer: đăng ký / đăng nhập / xem lịch sử đơn hàng.
   - Employee: phân quyền thao tác (tạo order, xem báo cáo, quản lý menu).

4. **Quản lý tài chính chuỗi & từng cửa hàng**:
   - **Doanh thu**:
     - Theo từng chi nhánh, theo ngày/tuần/tháng/năm.
     - Theo kênh bán (tại quầy, online, đối tác giao hàng).
     - Theo product/category (đồ uống nào bán chạy, giờ cao điểm).
   - **Chi phí nguyên vật liệu**:
     - Ghi nhận phiếu nhập kho (nhập sữa, cà phê, đường, topping, bao bì...).
     - Ghi nhận xuất kho theo đơn hàng (tiêu hao nguyên vật liệu theo recipe).
     - Tính tồn kho & giá vốn (COGS) cho từng chi nhánh.
   - **Báo cáo tài chính tổng hợp**:
     - Lãi gộp = Doanh thu bán hàng – Giá vốn hàng bán (COGS).
     - Kết hợp với chi phí lương & chi phí cố định (sau này).

5. **Quản lý nhân sự (lương & hợp đồng)**:
   - **Hồ sơ nhân viên**:
     - Hợp đồng lao động: loại hợp đồng, ngày bắt đầu/kết thúc, lương cơ bản, phụ cấp.
     - Thông tin chi nhánh làm việc, vị trí (barista, cashier, manager...).
   - **Lương & chấm công** (sau này có thể tách thành service riêng):
     - Ghi nhận ca làm, giờ làm thực tế.
     - Tính lương theo ca/giờ/tháng, cộng thưởng theo doanh thu chi nhánh (nếu có).
   - **Báo cáo nhân sự**:
     - Tổng chi phí lương theo chi nhánh, theo kỳ trả lương.

6. **Báo cáo & thống kê tổng hợp**:
   - Doanh thu theo ngày/tuần/tháng, theo chi nhánh, theo kênh.
   - Top sản phẩm bán chạy, biên lợi nhuận theo category/product.
   - Báo cáo chi phí nguyên vật liệu, lương, lợi nhuận ước tính.

---

## 4. Liên kết domain ↔ các microservice hiện tại

- `product` service:
  - Phụ trách domain **Menu**: Category / Product / Topping.
  - Cung cấp API cho:
    - Lấy danh sách menu cho app/POS.
    - Admin quản lý menu.

- `order` service:
  - Sẽ chứa Order / OrderItem / OrderItemTopping.
  - Sử dụng `product` service để validate product & price.
  - Có thể dùng Kafka để phát `ORDER_CREATED` event.

- `user` service:
  - Sẽ chứa User / Customer / Employee / Profile đầy đủ.
  - Cung cấp thông tin user/role cho các service khác (qua HTTP, không share DB).

- `auth` service:
  - Phụ trách **AuthN/AuthZ**:
    - Login/logout.
    - Sinh JWT access token (và sau này refresh token).
    - Validate token & trích xuất claim (username, roles).
  - Kết hợp với Spring Security:
    - Dùng JWT từ auth service để bảo vệ các API ở gateway / service con.
  - Không trực tiếp thao tác bảng user; khi cần verify credential sẽ gọi user-service.

- `gateway-service`:
  - Làm API Gateway chung cho frontend / POS.

---

## 5. Bước tiếp theo về code (ưu tiên)

1. **Hoàn thiện module `product` theo nghiệp vụ menu đồ uống**:
   - Thêm service + controller:
     - API CRUD cho `Category`, `Product`, `Topping`.
     - API **lấy menu hiển thị** (chỉ ACTIVE, theo category/size).
   - Ánh xạ `Product` hiện tại thành **đồ uống trong menu** (sử dụng sẵn `ProductSize`, `ProductStatus`, `ToppingInfo`).

2. **Thiết kế entity Order cho module `order`**:
   - `Order`, `OrderItem`, `OrderItemTopping` extends `BaseEntity`.
   - Repository + service basic.

3. **Thiết kế lại model `user`**:
   - Thêm field & annotation JPA đầy đủ.
   - Chuẩn bị cho Spring Security (sau này).

File này sẽ được cập nhật dần khi chúng ta refine thêm nghiệp vụ (khuyến mãi, loyalty, chi nhánh, ca làm, v.v.). Đây là “bản đồ domain” để đối chiếu khi thiết kế API và database.


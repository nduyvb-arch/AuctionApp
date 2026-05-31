# Hệ thống đấu giá trực tuyến - Nhóm 2
[![Maven CI/CD](https://github.com/nduyvb-arch/AuctionApp/actions/workflows/maven-ci.yml/badge.svg?branch=main)](https://github.com/nduyvb-arch/AuctionApp/actions/workflows/maven-ci.yml)
## 1. Giới thiệu đề tài

Đây là ứng dụng đấu giá trực tuyến được xây dựng bằng JavaFX theo mô hình Client-Server.  
Hệ thống cho phép người dùng đăng ký, đăng nhập, tham gia đấu giá sản phẩm, đặt giá theo thời gian thực, theo dõi lịch sử đấu giá và sử dụng các chức năng tương ứng với từng vai trò.

Ứng dụng hỗ trợ ba nhóm người dùng chính:

- Người đấu giá: xem sản phẩm, tham gia phòng đấu giá, đặt giá, xem lịch sử đấu giá, nạp tiền.
- Người bán: đăng sản phẩm, quản lý sản phẩm đã đăng, bắt đầu phiên đấu giá, xem lịch sử bán hàng.
- Quản trị viên: quản lý người dùng, khóa/mở khóa tài khoản, quản lý sản phẩm và phiên đấu giá.

## 2. Phạm vi hệ thống

Hệ thống tập trung vào các nghiệp vụ chính của một nền tảng đấu giá:

- Quản lý tài khoản người dùng.
- Quản lý sản phẩm đấu giá.
- Tổ chức phiên đấu giá theo thời gian.
- Đặt giá và cập nhật giá hiện tại.
- Cập nhật dữ liệu realtime giữa nhiều client.
- Hiển thị biểu đồ lịch sử đặt giá.
- Chat trong phòng đấu giá.
- Quản lý người dùng và phiên đấu giá ở phía admin.

## 3. Công nghệ sử dụng
- AI hỗ trợ: Gemini, Claude, ChatGPT, Github Copilot
- Ngôn ngữ lập trình: Java
- Giao diện: JavaFX, CSS
- Quản lý project: Maven
- Cơ sở dữ liệu: MySQL
- Mô hình mạng: Client-Server sử dụng Socket
- Kiểm thử: JUnit 5
- Mã hóa mật khẩu: BCrypt
- Logging: SLF4J, Logback

## 4. Yêu cầu cài đặt

Trước khi chạy chương trình, cần cài đặt:

- JDK 25 hoặc mới hơn
- Maven 3.9 trở lên
- MySQL Server 8.x
- IDE khuyến nghị: IntelliJ IDEA

Lưu ý: Project đang sử dụng JavaFX 25.0.3 và cấu hình `maven.compiler.source` là `25`, vì vậy cần dùng JDK 25 để tránh lỗi phiên bản Java.

## 5. Cấu trúc thư mục

```text
BTL/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── client/              # Ứng dụng client JavaFX
│   │   │   ├── client/controllers/  # Controller cho các màn hình người dùng
│   │   │   ├── client/controllers/admin/ # Controller cho giao diện admin
│   │   │   ├── client/network/      # Kết nối client tới server
│   │   │   ├── common/              # Message và model dùng chung
│   │   │   ├── common/model/        # User, Item, AuctionStatus, ChatMessage
│   │   │   ├── server/              # Logic server
│   │   │   ├── server/data/         # DatabaseManager, ItemDAO
│   │   │   ├── server/manager/      # AuctionManager, UserManager, AutoBid, AntiSniper
│   │   │   └── server/network/      # ServerMain, AuctionServer, ClientHandler
│   │   └── resources/
│   │       ├── org/example/client/views/       # Các file FXML
│   │       ├── org/example/client/views/admin/ # FXML cho admin
│   │       ├── images/                         # Icon, ảnh giao diện
│   │       └── styles/                         # CSS
│   └── test/                       # Unit test
├── images/                         # Ảnh minh họa, class diagram
├── pom.xml                         # Cấu hình Maven
├── checkstyle.xml
└── README.md

## 6. Cấu hình file `.env`

Trước khi chạy chương trình, cần tạo file `.env` ở thư mục gốc của project, cùng cấp với file `pom.xml`.

Cấu trúc thư mục đúng:

```text
BTL/
├── pom.xml
├── .env
├── src/
└── README.md
```

Nội dung file `.env`:

```env
DB_URL=jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=your_password
```

Trong đó:

* `DB_URL`: đường dẫn kết nối tới database MySQL.
* `DB_USER`: tên tài khoản MySQL.
* `DB_PASSWORD`: mật khẩu MySQL.

## 7. Câu lệnh dòng lệnh để chạy chương trình

Project sử dụng JavaFX 25.0.3 và cấu hình Java 25 trong `pom.xml`, vì vậy nên dùng JDK 25 hoặc mới hơn.

Biên dịch project:

```bash
mvn clean compile
```

Chạy kiểm thử:

```bash
mvn test
```

Chạy server:

<img src="images/img.png" width="600">

Chạy client:

```bash
mvn javafx:run
```

Lưu ý: tất cả các câu lệnh trên cần được chạy tại thư mục gốc của project, tức là thư mục chứa file `pom.xml`.


Nếu client chạy trên máy khác với máy đang chạy server, cần sửa địa chỉ server trong file:

```text
src/main/java/org/example/client/ClientApp.java
```

Tìm dòng:

```java
private static final String SERVER_ADDRESS = "localhost";
```

Thay `localhost` bằng địa chỉ IP của máy đang chạy server, ví dụ:

```java
private static final String SERVER_ADDRESS = "192.168.1.10";
```

Sau đó chạy lại client bằng:

```bash
mvn javafx:run
```

## 8. Danh sách chức năng đã hoàn thành

### 8.1. Chức năng chung

* Đăng ký tài khoản người dùng.
* Đăng nhập vào hệ thống.
* Phân quyền người dùng theo vai trò: người đấu giá, người bán và quản trị viên.
* Chọn giao diện sử dụng theo vai trò sau khi đăng nhập.
* Xem thông tin tài khoản.
* Hiển thị số dư tài khoản.
* Nạp tiền vào tài khoản.
* Rút tiền khỏi tài khoản.
* Đăng xuất khỏi hệ thống.

### 8.2. Chức năng người đấu giá

* Xem danh sách sản phẩm đang có trong hệ thống.
* Xem chi tiết sản phẩm đấu giá.
* Tham gia phòng đấu giá.
* Đặt giá cho sản phẩm.
* Kiểm tra điều kiện đặt giá hợp lệ
* Không cho phép đặt giá khi phiên đấu giá đã kết thúc.
* Không cho phép cùng một người dùng đặt giá hai lần liên tiếp cho cùng một sản phẩm.
* Xem lịch sử đấu giá của bản thân.
* Xem biểu đồ lịch sử đặt giá.
* Chat với các người đấu giá khác trong phòng đấu giá.

### 8.3. Chức năng người bán

* Đăng sản phẩm mới.
* Nhập thông tin sản phẩm: tên, mô tả, loại sản phẩm, giá khởi điểm, bước giá và thời gian kết thúc.
* Tải ảnh sản phẩm.
* Xem danh sách sản phẩm do mình đăng.
* Bắt đầu phiên đấu giá.
* Theo dõi trạng thái sản phẩm.
* Xem lịch sử bán hàng.

### 8.4. Chức năng quản trị viên

* Truy cập giao diện quản trị.
* Xem danh sách người dùng trong hệ thống.
* Khóa tài khoản người dùng.
* Mở khóa tài khoản người dùng.
* Quản lý danh sách sản phẩm.
* Quản lý các phiên đấu giá.
* Xem thống kê tổng quan của hệ thống.

### 8.5. Chức năng realtime

* Cập nhật sản phẩm mới giữa nhiều client.
* Cập nhật giá đấu mới giữa nhiều client.
* Cập nhật biểu đồ giá sau khi có lượt đặt giá mới.
* Cập nhật trạng thái phiên đấu giá.
* Gửi và nhận tin nhắn chat trong phòng đấu giá.
* Đăng xuất người dùng ngay khi tài khoản bị admin khóa.

### 8.6. Chức năng lưu trữ dữ liệu

* Lưu thông tin người dùng vào MySQL.
* Lưu thông tin sản phẩm vào MySQL.
* Lưu lịch sử đặt giá vào MySQL.
* Tự động kiểm tra và tạo bảng dữ liệu khi server kết nối database.
* Đọc thông tin cấu hình database từ file `.env`.

## 9. Link báo cáo PDF và video demo
```markdown
- Báo cáo PDF: https://drive.google.com/...
- Video demo: https://youtu.be/...
```
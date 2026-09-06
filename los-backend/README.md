# ⚙️ LOS Backend Service

Dịch vụ Backend cốt lõi trong hệ thống Loan Origination System (LOS), chịu trách nhiệm xử lý nghiệp vụ tài chính, thẩm định tín dụng tự động, xác thực bảo mật và quản lý hồ sơ vay vốn.

---

## ⚡ Tính Năng Chính

* **Xác thực & Phân quyền**: Đăng ký, đăng nhập an toàn bằng JWT (Access Token) kết hợp cơ chế xoay vòng Refresh Token tự động qua HttpOnly Cookie.
* **Dịch vụ OTP Email**: Tự động khởi tạo và gửi mã xác thực bảo mật 6 chữ số qua Gmail SMTP TLS để khách hàng ký số hợp đồng vay.
* **Chấm điểm tín dụng tự động (Credit Scoring)**: Đánh giá hồ sơ theo thang điểm.
* **Đánh giá khả năng trả nợ (DTI)**: Tính toán chính xác tỷ lệ nợ trên thu nhập (Debt-to-Income).
* **Ra quyết định phê duyệt tức thì**: Tự động xét duyệt và gán trạng thái hồ sơ (`APPROVED` hoặc `REJECTED`) ngay sau khi khách hàng nộp đơn.
* **Quản lý & Tra cứu hồ sơ**: Cung cấp RESTful API quản lý hồ sơ vay và cho phép tra cứu toàn bộ lịch sử nộp đơn theo số định danh CCCD.
* **Bộ Unit Test hoàn chỉnh**: Tích hợp sẵn 63 unit test bao phủ toàn bộ các dịch vụ nghiệp vụ cốt lõi và các trường hợp biên (edge cases).

---

## 🛠 Công Nghệ

* **Ngôn ngữ & Framework**: Java 21, Spring Boot 3.5
* **Cơ sở dữ liệu & ORM**: MySQL 8.x, Spring Data JPA, Hibernate
* **Bảo mật**: Spring Security 6, Nimbus JOSE JWT, BCrypt Password Encoder
* **Dịch vụ Email**: Spring Boot Starter Mail (Gmail SMTP TLS)

---

## 📁 Cấu Trúc Thư Mục

```text
los-backend/
├── src/main/java/com/example/los/
│   ├── controller/      # REST API Controllers (Auth, Loan, OTP, Assessment)
│   ├── service/         # Nghiệp vụ: Thẩm định (Assessment), Vay (Loan), OTP, Email, Auth
│   ├── entity/          # JPA Entities (User, LoanApplication, LoanProduct, Policy,...)
│   ├── repository/      # Spring Data JPA Repositories
│   ├── config/          # Spring Security, JWT Filter, CORS & Static Resource Forwarder
│   └── exception/       # Global Exception Handler
├── src/test/java/       # Unit Tests kiểm thử nghiệp vụ
├── .env                 
├── pom.xml             
```

---

## 🚀 Hướng Dẫn Chạy Nhanh

### 1. Chuẩn bị cơ sở dữ liệu (MySQL)
Tạo database trên MySQL (Port 3306):
```sql
CREATE DATABASE IF NOT EXISTS los CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Cấu hình biến môi trường
Tạo file `los-backend/.env`:
```env
DB_URL=jdbc:mysql://localhost:3306/los?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=mat_khau_mysql_cua_ban
MAIL_USERNAME=email_cua_ban@gmail.com
MAIL_PASSWORD=mat_khau_ung_dung_gmail_16_ky_tu
JWT_SECRET=chuoi_khoa_bi_mat_jwt_dai_tren_32_ky_tu_123456789
JWT_EXPIRATION_MS=86400000
```

### 3. Khởi chạy ứng dụng (Port 8080)
```powershell
# Chạy nhanh bằng script tự nạp .env
cd los-backend
.\run-demo.ps1
```

### 4. Chạy Unit Test
```powershell
.\mvnw.cmd test
```

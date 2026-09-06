# 🏦 Loan Origination System (LOS)

Hệ thống thẩm định và quản lý khoản vay trực tuyến toàn diện, tích hợp giữa **Web Portal khách hàng** (React 19 + Tailwind CSS) và **Core thẩm định tín dụng tự động** (Spring Boot 3 + MySQL).

---

## ⚡ Tính Năng Chính

* **Dự toán khoản vay thông minh**: Cho phép tùy chỉnh hạn mức (3 - 100 triệu VNĐ) và kỳ hạn, tự động tính toán lãi suất cố định cùng số tiền trả góp hàng tháng theo dư nợ giảm dần.
* **Đăng ký hồ sơ vay trực tuyến**: Thu thập thông tin định danh (CCCD), nghề nghiệp, mức thu nhập và người tham chiếu khẩn cấp qua biểu mẫu đa bước có kiểm tra dữ liệu chặt chẽ.
* **Tự động sinh hợp đồng**: Kết xuất hợp đồng tín dụng và đơn đề nghị vay vốn đúng chuẩn biểu mẫu pháp lý, hỗ trợ xem trước và tải file PDF trực tiếp trên trình duyệt.
* **Ký hợp đồng bằng mã OTP**: Gửi mã xác thực 6 chữ số đến email người vay qua dịch vụ Gmail SMTP TLS để hoàn tất ký kết hồ sơ.
* **Thẩm định tín dụng tự động (Underwriting Engine)**: Tự động chấm điểm hồ sơ theo ma trận nghề nghiệp/thu nhập và kiểm tra tỷ lệ nợ trên thu nhập để ra quyết định tức thì.
* **Tra cứu hồ sơ thời gian thực**: Khách hàng tra cứu toàn bộ lịch sử khoản vay.
* **Bảo mật**: Xác thực người dùng bằng Spring Security 6 và JWT, kết hợp cơ chế Access/Refresh Token an toàn qua HttpOnly Cookie.

---

## 🛠 Công Nghệ

| Phân hệ | Công nghệ chính |
| :--- | :--- |
| **Frontend Portal** | React 19, TypeScript, Vite, Tailwind CSS 4, Lucide Icons, jsPDF, html-to-image |
| **Backend Service** | Java 21, Spring Boot 3.5, Spring Data JPA, Hibernate, Spring Security 6, Nimbus JOSE JWT |
| **Cơ sở dữ liệu** | MySQL 8.x |
| **Dịch vụ Email** | Spring Mail (Gmail SMTP TLS) gửi mã OTP |
| **Công cụ Build & Chạy** | Maven Wrapper (`mvnw`), npm, PowerShell Scripts (`build-demo.ps1`, `run-demo.ps1`) |

---

## 📁 Cấu Trúc Thư Mục

```text
Loan-Origination-System-Portal/
├── los-portal/           # Phân hệ Frontend Web Portal (React 19 + TypeScript + Vite)
│   ├── src/components/   # HomePage, ApplicationPage, LookupPage, AccountPage
│   └── src/services/     # Tầng gọi API kết nối Backend
├── los-backend/          # Phân hệ Backend Service (Spring Boot 3 + Java 21 + MySQL)
│   ├── src/main/java/    # Controller, Service, Entity, Repository, Security
│   └── src/main/resources/static/ # File build Portal sau khi đóng gói All-in-One
├── MySQL/                # Cơ sở dữ liệu
│   └── init_schema.sql   # Kịch bản DDL & DML khởi tạo bảng & dữ liệu mẫu
├── build-demo.ps1        # Script tự động đóng gói cả Portal và Backend thành 1 file JAR
├── run-demo.ps1          # Script 1-click khởi chạy toàn bộ hệ thống
└── .gitignore            # Cấu hình bỏ qua các file nhạy cảm và build artifacts
```

---

## 🚀 Hướng Dẫn Chạy Nhanh

### 1. Chuẩn bị
* Tạo database và chạy kịch bản khởi tạo bảng tại `MySQL/init_schema.sql`.
* Cấu hình thông tin kết nối database và email trong file `los-backend/.env` (tạo từ `.env.example`).

### 2. Đóng gói & Khởi chạy (All-In-One)
```powershell
# 1. Đóng gói trọn gói cả Portal và Backend thành 1 file JAR duy nhất
.\build-demo.ps1

# 2. Khởi chạy hệ thống (tự động mở trình duyệt tại http://localhost:8080)
.\run-demo.ps1
```

*(Hoặc khởi chạy chế độ Dev song song: `.\run-demo.ps1 -Dev`)*

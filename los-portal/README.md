# 🌐 LOS Customer Web Portal

Cổng thông tin khách hàng (Customer Portal) trong hệ thống Loan Origination System (LOS), hỗ trợ người dùng đăng ký khoản vay, xem trước hợp đồng, xác thực OTP và tra cứu kết quả thẩm định trực tuyến.

---

## ⚡ Tính Năng Chính

* **Dự toán khoản vay**: Thanh trượt hạn mức (3 - 100 triệu VNĐ) và kỳ hạn linh hoạt, tự động tính lãi suất cố định cùng số tiền trả góp hàng tháng theo dư nợ giảm dần.
* **Đăng ký hồ sơ trực tuyến**: Biểu mẫu đa bước thu thập thông tin cá nhân (CCCD/CMND), nghề nghiệp, mức thu nhập và người liên hệ khẩn cấp kèm kiểm tra hợp lệ dữ liệu thời gian thực.
* **Tạo hợp đồng & Xuất PDF**: Tự động kết xuất hợp đồng tín dụng và đơn đề nghị vay vốn đúng chuẩn pháp lý, cho phép xem trực tiếp và tải file PDF về máy.
* **Xác thực ký số qua OTP**: Tích hợp gửi mã xác thực bảo mật 6 chữ số đến email người vay trước khi ký hợp đồng và gửi hồ sơ duyệt.
* **Tra cứu tiến độ hồ sơ theo CCCD**: Theo dõi trạng thái toàn bộ các khoản vay theo dòng thời gian trực quan (*Tiếp nhận ➔ Thẩm định & Chấm điểm ➔ Phê duyệt / Từ chối*).
* **Quản lý tài khoản & Bảo mật**: Đăng nhập, đăng ký an toàn bằng JWT kết hợp cơ chế tự động xoay vòng Refresh Token qua HttpOnly Cookie.

---

## 🛠 Công Nghệ

* **Nền tảng**: React 19, TypeScript, Vite
* **Giao diện**: Tailwind CSS 4, Lucide React
* **Tiện ích xuất file**: jsPDF, html-to-image, html2canvas
* **Giao tiếp API**: Fetch API kết hợp interceptor xử lý refresh token tự động

---

## 📁 Cấu Trúc Thư Mục

```text
los-portal/
├── src/
│   ├── components/
│   │   ├── portal/
│   │   │   ├── HomePage.tsx         # Trang chủ & bảng tính dự toán khoản vay
│   │   │   ├── ApplicationPage.tsx  # Form nộp hồ sơ 4 bước, hợp đồng & OTP
│   │   │   ├── LookupPage.tsx       # Tra cứu tiến độ hồ sơ theo CCCD
│   │   │   └── AccountPage.tsx      # Quản lý tài khoản khách hàng
│   │   └── CustomerPortal.tsx       # Khung điều hướng chính (Shell layout)
│   ├── services/
│   │   └── api.ts                   # Gọi API backend (Auth, Loan, OTP)
│   ├── App.tsx                      # Component gốc
│   └── main.tsx                     # Điểm khởi tạo ứng dụng
├── .env.example                     # Mẫu biến môi trường
└── package.json                     # Danh sách dependencies & scripts
```

---

## 🚀 Hướng Dẫn Chạy Nhanh

```bash
# 1. Cài đặt dependencies
cd los-portal
npm install

# 2. Tạo file cấu hình (nếu cần đổi cổng API backend)
cp .env.example .env

# 3. Khởi chạy môi trường phát triển (Port 3000)
npm run dev

# 4. Đóng gói production (ra thư mục dist/)
npm run build
```

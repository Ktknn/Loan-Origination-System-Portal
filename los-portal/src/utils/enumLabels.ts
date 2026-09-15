/**
 * enumLabels.ts — Map enum name (gửi lên backend) → label tiếng Việt (hiển thị UI).
 * Dùng hàm label() để convert bất kỳ chỗ nào cần hiển thị cho user.
 */

export const ENUM_LABELS: Record<string, string> = {
  // Gender
  Nam: 'Nam',
  Nu: 'Nữ',

  // IncomeRange
  DUOI_10_TRIEU:    'Dưới 10 triệu',
  TU_10_DEN_20_TRIEU: '10 - 20 triệu',
  TU_20_DEN_30_TRIEU: '20 - 30 triệu',
  TREN_30_TRIEU:    'Trên 30 triệu',

  // LoanPurpose
  MUA_PHUONG_TIEN:   'Mua phương tiện đi lại',
  MUA_SAM_DO_DUNG:   'Mua sắm đồ dùng sinh hoạt',
  HOC_TAP:           'Học tập',
  CHUA_BENH:         'Chữa bệnh',
  DU_LICH:           'Du lịch',
  VAY_TIEU_DUNG_KHAC:'Tiêu dùng khác',

  // Occupation
  CAN_BO_CONG_CHUC:  'Cán bộ / Công chức',
  HUU_TRI:           'Hưu trí',
  SINH_VIEN:         'Sinh viên',
  KINH_DOANH_TU_DO:  'Kinh doanh tự do',
  NHAN_VIEN_CONG_TY: 'Nhân viên công ty',
  KHAC:              'Khác',

  // LoanStatus
  DRAFT:     'Nháp',
  SUBMITTED: 'Đã nộp',
  PENDING:   'Đang xử lý',
  APPROVED:  'Đã duyệt',
  REJECTED:  'Từ chối',
};

/** Trả về label tiếng Việt. Nếu không có mapping thì trả về nguyên giá trị. */
export const label = (val: string | null | undefined): string =>
  val ? (ENUM_LABELS[val] ?? val) : '';

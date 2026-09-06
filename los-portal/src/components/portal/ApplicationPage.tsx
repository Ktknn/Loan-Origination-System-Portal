import { useState, useRef } from 'react';
import { toPng } from 'html-to-image';
import { jsPDF } from 'jspdf';
import {
  FileText, CreditCard, CheckCircle2, ArrowRight, Edit3,
  ChevronLeft, Download, Eye, Smartphone, Mail, Loader2
} from 'lucide-react';
import { ViewType, FormData } from './shared/types';
import { sendOtpAPI, verifyOtpAPI, submitLoanAPI } from '../../services/api';

// =============================================
// LOAN TIER CONFIG
// =============================================
const RATE_MATRIX: Record<string, number> = {
  '3000000_1': 5,
  '3000000_3': 5,
  '3000000_6': 5,
  '4000000_3': 5,
  '5000000_3': 5,
  '6000000_3': 4.08,
  '6000000_4': 3.87,
  '6000000_5': 3.73,
  '6000000_6': 3.67,
  '7000000_3': 4.08,
  '7000000_4': 3.87,
  '7000000_5': 3.73,
  '7000000_6': 3.68,
  '8000000_3': 4.08,
  '8000000_4': 3.86,
  '8000000_5': 3.74,
  '8000000_6': 3.67,
  '8000000_9': 3.6,
  '9000000_3': 4.08,
  '9000000_4': 3.86,
  '9000000_5': 3.74,
  '9000000_6': 3.68,
  '9000000_9': 3.6,
  '10000000_6': 2.78,
  '10000000_9': 2.7,
  '12000000_3': 4.08,
  '12000000_4': 3.86,
  '12000000_5': 3.74,
  '12000000_6': 3.67,
  '12000000_9': 3.6,
  '12000000_12': 3.6,
  '12000000_15': 3.63,
  '15000000_9': 2.7,
  '15000000_12': 2.69,
  '15000000_15': 2.7,
  '20000000_12': 2.69,
  '20000000_15': 2.7,
  '20000000_18': 2.72,
  '50000000_18': 2.72,
  '50000000_21': 2.76,
  '50000000_24': 2.79,
  '50000000_30': 2.87,
  '50000000_36': 2.95,
  '50000000_42': 3.03,
  '50000000_48': 3.1,
  '60000000_15': 2.7,
  '60000000_18': 2.72,
  '60000000_21': 2.75,
  '60000000_24': 2.79,
  '60000000_27': 2.83,
  '60000000_30': 2.87,
  '60000000_36': 2.95,
  '70000000_18': 2.72,
  '70000000_21': 2.75,
  '70000000_24': 2.79,
  '70000000_27': 2.83,
  '70000000_30': 2.87,
  '70000000_36': 2.95,
  '80000000_21': 2.2,
  '80000000_24': 2.23,
  '80000000_27': 2.25,
  '80000000_30': 2.28,
  '80000000_36': 2.33,
  '90000000_24': 2.23,
  '90000000_27': 2.25,
  '90000000_30': 2.28,
  '90000000_36': 2.23,
  '100000000_30': 2.28,
  '100000000_36': 2.33,
  '100000000_42': 2.39,
  '100000000_48': 2.44,
};

const ALLOWED_AMOUNTS = [3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 50, 60, 70, 80, 90, 100];

const getAvailableTerms = (amountMillions: number): number[] => {
  const prefix = `${amountMillions * 1000000}_`;
  return Object.keys(RATE_MATRIX)
    .filter(k => k.startsWith(prefix))
    .map(k => parseInt(k.split('_')[1]))
    .sort((a, b) => a - b);
};

const formatAmount = (millions: number): string => {
  const vnd = millions * 1_000_000;
  return vnd.toLocaleString('vi-VN');
};

const sliderToMillions = (sliderVal: number): number => {
  return ALLOWED_AMOUNTS[sliderVal] || 3;
};

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '';
  if (dateStr.includes('/')) return dateStr;
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
};

// =============================================
// CONTRACT DOCUMENT (A + B)
// =============================================
const ContractDocument = ({ formData, interestRate }: { formData: FormData, interestRate: number }) => {
  const contractStyle: React.CSSProperties = {
    fontFamily: '"Times New Roman", Times, serif',
  };
  const today = new Date();
  const dateStr = `${today.getDate()} tháng ${today.getMonth() + 1} năm ${today.getFullYear()}`;

  return (
    <div className="space-y-6">
      {/* PHẦN A */}
      <div id="contract-part-a" className="bg-white border border-slate-200 rounded-lg p-6 sm:p-8 text-[13.5px] text-slate-800 leading-7 space-y-5 overflow-x-auto" style={contractStyle}>

        <div className="text-center space-y-1">
          <p className="font-bold text-[15px] uppercase tracking-wide">A. ĐƠN ĐỀ NGHỊ VAY VỐN</p>
          <p className="text-slate-500 text-xs">Hà Nội, ngày {dateStr}</p>
        </div>

        <div className="border-t border-slate-200 pt-4 space-y-3">
          <p className="font-bold uppercase tracking-wide">I. KHÁCH HÀNG</p>
          <div className="space-y-1.5">
            <p className="font-semibold pl-2">1. Thông tin cá nhân</p>
            <p className="pl-6">1.1. Họ và tên: <span className="font-semibold">{formData.fullName}</span></p>
            <p className="pl-6">1.2. Ngày sinh: {formatDate(formData.dob)}</p>
            <p className="pl-6">1.3. Số điện thoại: {formData.phone}</p>
            <p className="pl-6">1.4. CCCD/CMND: {formData.cccd}</p>
            <p className="pl-6">1.5. Email liên hệ: {formData.email}</p>
            <p className="pl-6">1.6. Nghề nghiệp: {formData.occupation}</p>
            <p className="pl-6">1.7. Địa chỉ thường trú: {formData.address}, {formData.ward}, {formData.district}, {formData.city}</p>
          </div>
          <div className="space-y-1.5">
            <p className="font-semibold pl-2">2. Đề nghị vay vốn</p>
            <p className="pl-6">2.1. Số tiền đề nghị vay: <span className="font-bold">{formData.amount.replace(/\s*(VND|VNĐ)/gi, '')} VNĐ</span></p>
            <p className="pl-6">2.2. Mục đích vay: {formData.purpose}</p>
            <p className="pl-6">2.3. Thời hạn vay: <span className="font-semibold">{formData.term} tháng</span></p>
          </div>
        </div>

        <div className="border-t border-slate-200 pt-4 space-y-1.5">
          <p className="font-bold uppercase tracking-wide">II. CAM KẾT CỦA KHÁCH HÀNG</p>
          <p className="pl-4">1. Sử dụng vốn vay đúng mục đích đã đăng ký.</p>
          <p className="pl-4">2. Hoàn trả đầy đủ gốc và lãi đúng kỳ hạn.</p>
          <p className="pl-4">3. Thông báo kịp thời cho bên cho vay khi có thay đổi về tài chính hoặc việc làm.</p>
          <p className="pl-4">4. Chịu trách nhiệm về tính trung thực của toàn bộ thông tin cung cấp.</p>
        </div>

        <div className="border-t border-slate-200 pt-5 text-right">
          <p className="font-bold">NGƯỜI ĐỀ NGHỊ VAY</p>
          <p className="mt-10 border-b border-slate-400 inline-block w-40 text-slate-300">................................</p>
          <p className="text-slate-500 text-xs mt-1">{formData.fullName}</p>
        </div>
      </div>

      {/* PHẦN B */}
      <div id="contract-part-b" className="bg-white border border-slate-200 rounded-lg p-6 sm:p-8 text-[13.5px] text-slate-800 leading-7 space-y-5 overflow-x-auto" style={contractStyle}>
        <div className="text-center space-y-1">
          <p className="font-bold text-[15px] uppercase tracking-wide">B. HỢP ĐỒNG CHO VAY</p>
          <p>Số hợp đồng: <span className="font-semibold">HD-{today.getFullYear()}-XXXXX</span></p>
          <p className="text-slate-500 text-xs">Hà Nội, ngày {dateStr}</p>
        </div>

        <div className="border-t border-slate-200 pt-4 space-y-1">
          <p className="font-bold">CÁC BÊN THAM GIA HỢP ĐỒNG</p>
          <p className="pl-4"><span className="font-semibold">BÊN CHO VAY (Bên A):</span> Công ty Tài chính XXX</p>
          <p className="pl-4 mt-1"><span className="font-semibold">BÊN VAY (Bên B):</span> {formData.fullName} — <span className="italic text-slate-500">(thông tin chi tiết theo Phần A)</span></p>
        </div>

        <div className="border-t border-slate-200 pt-4 space-y-1.5">
          <p className="font-bold uppercase tracking-wide">I. NỘI DUNG KHOẢN VAY</p>
          <p className="pl-4">1. Số tiền vay: <span className="font-bold">{formData.amount.replace(/\s*(VND|VNĐ)/gi, '')} VNĐ</span></p>
          <p className="pl-4">2. Lãi suất: <span className="font-semibold">{interestRate.toString().replace('.', ',')}%/tháng</span> tính trên dư nợ giảm dần.</p>
          <p className="pl-4">3. Thời hạn: <span className="font-semibold">{formData.term} tháng</span> kể từ ngày giải ngân.</p>
          <p className="pl-4">4. Mục đích: {formData.purpose}.</p>
          <p className="pl-4">5. Đồng tiền cho vay: <span className="font-semibold">Việt Nam Đồng (VNĐ)</span>.</p>
        </div>

        <div className="border-t border-slate-200 pt-6 grid grid-cols-2 gap-8 text-center">
          <div className="space-y-14">
            <p className="font-bold">ĐẠI DIỆN BÊN A</p>
            <div>
              <p className="border-b border-slate-400 pb-0.5 text-slate-300">................................</p>
            </div>
          </div>
          <div className="space-y-14">
            <p className="font-bold">BÊN VAY (Bên B)</p>
            <div>
              <p className="border-b border-slate-400 pb-0.5 text-slate-300">................................</p>
              <p className="text-slate-500 text-xs mt-1">{formData.fullName}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// =============================================
// APPLICATION PAGE
// =============================================
interface ApplicationPageProps {
  formData: FormData;
  setFormData: React.Dispatch<React.SetStateAction<FormData>>;
  setCurrentView: (v: ViewType) => void;
}

export const ApplicationPage = ({ formData, setFormData, setCurrentView }: ApplicationPageProps) => {
  const [subView, setSubView] = useState<'APPLY' | 'CONFIRM' | 'OTP' | 'SUCCESS'>('APPLY');
  const [isAgreed, setIsAgreed] = useState(false);
  const [loanSlider, setLoanSlider] = useState(0);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [apiError, setApiError] = useState('');

  const [otpCode, setOtpCode] = useState('');
  const [isSendingOTP, setIsSendingOTP] = useState(false);

  const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);

  const handleDownloadPDF = async () => {
    setIsGeneratingPDF(true);
    try {
      const partA = document.getElementById('contract-part-a');
      const partB = document.getElementById('contract-part-b');
      if (!partA || !partB) return;

      const pdf = new jsPDF('p', 'mm', 'a4');
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfPageHeight = pdf.internal.pageSize.getHeight();

      const addImageToPdf = (imgData: string, node: HTMLElement, isFirst: boolean) => {
        if (!isFirst) pdf.addPage();
        const aspectRatio = node.offsetHeight / node.offsetWidth;
        const pdfImageHeight = pdfWidth * aspectRatio;

        let heightLeft = pdfImageHeight;
        let position = 0;

        pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, pdfImageHeight);
        heightLeft -= pdfPageHeight;

        while (heightLeft > 0) {
          position -= pdfPageHeight;
          pdf.addPage();
          pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, pdfImageHeight);
          heightLeft -= pdfPageHeight;
        }
      };

      const imgDataA = await toPng(partA, { cacheBust: true, pixelRatio: 2 });
      addImageToPdf(imgDataA, partA, true);

      const imgDataB = await toPng(partB, { cacheBust: true, pixelRatio: 2 });
      addImageToPdf(imgDataB, partB, false);

      pdf.save(`HopDongVay_${formData.cccd || 'Draft'}.pdf`);
    } catch (error: any) {
      console.error('Lỗi khi tạo PDF:', error);
      alert('Có lỗi xảy ra khi tạo PDF: ' + (error?.message || error));
    } finally {
      setIsGeneratingPDF(false);
    }
  };

  const amountNum = parseInt(formData.amount.replace(/\D/g, '')) || 0;
  const termNum = parseInt(formData.term) || 1;
  const interestRate = RATE_MATRIX[`${amountNum}_${termNum}`] || 5.0; // Dynamic interest rate or default 5.0%
  const monthlyPayment = (amountNum / termNum) + (amountNum * (interestRate / 100));
  const formattedPayment = new Intl.NumberFormat('vi-VN').format(Math.round(monthlyPayment)) + ' VNĐ';

  const loanMillions = sliderToMillions(loanSlider);
  const availableTerms = getAvailableTerms(loanMillions);

  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  const handleSliderChange = (sliderVal: number) => {
    setLoanSlider(sliderVal);
    const millions = sliderToMillions(sliderVal);
    const terms = getAvailableTerms(millions);
    const currentTerm = parseInt(formData.term);
    const newTerm = terms.includes(currentTerm) ? String(currentTerm) : String(terms[0] ?? '');
    setFormData(prev => ({ ...prev, amount: formatAmount(millions), term: newTerm }));
    if (errors.amount) setErrors(prev => ({ ...prev, amount: '' }));
  };

  const inputCls = (field: string) =>
    `w-full bg-white border ${errors[field] ? 'border-red-400 focus:ring-red-400' : 'border-slate-300 focus:ring-blue-500 focus:border-blue-500'} text-slate-800 rounded-lg px-3 py-1.5 text-[13px] focus:ring-2 outline-none transition placeholder:text-slate-400`;

  const isVnPhone = (p: string) => /^(0[35789])[0-9]{8}$/.test(p.trim());
  const isValidCccd = (c: string) => /^([0-9]{9}|[0-9]{12})$/.test(c.trim());
  const isValidEmail = (e: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e.trim());

  const handleProceed = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.amount) newErrors.amount = 'Vui lòng nhập số tiền muốn vay';
    if (!formData.term) newErrors.term = 'Vui lòng chọn kỳ hạn';
    if (!formData.fullName) newErrors.fullName = 'Vui lòng nhập họ và tên';
    if (!formData.gender) newErrors.gender = 'Vui lòng chọn giới tính';
    if (!formData.purpose) newErrors.purpose = 'Vui lòng chọn mục đích vay';

    if (!formData.cccd) {
      newErrors.cccd = 'Vui lòng nhập CCCD/CMND';
    } else if (!isValidCccd(formData.cccd)) {
      newErrors.cccd = 'CCCD/CMND không hợp lệ (9 hoặc 12 số)';
    }

    if (!formData.phone) {
      newErrors.phone = 'Vui lòng nhập số điện thoại';
    } else if (!isVnPhone(formData.phone)) {
      newErrors.phone = 'Số điện thoại không hợp lệ';
    }

    if (!formData.email) {
      newErrors.email = 'Vui lòng nhập email';
    } else if (!isValidEmail(formData.email)) {
      newErrors.email = 'Email không hợp lệ';
    }

    if (!formData.address) newErrors.address = 'Vui lòng nhập địa chỉ/số nhà';
    if (!formData.city) newErrors.city = 'Vui lòng nhập Tỉnh/Thành phố';
    if (!formData.ward) newErrors.ward = 'Vui lòng nhập Phường/Xã';
    if (!formData.occupation) newErrors.occupation = 'Vui lòng chọn nghề nghiệp';
    if (!formData.income) newErrors.income = 'Vui lòng chọn mức thu nhập';
    if (!formData.ec1Name) newErrors.ec1Name = 'Vui lòng nhập tên người liên hệ 1';
    if (!formData.ec1Relation) newErrors.ec1Relation = 'Vui lòng chọn quan hệ';

    if (!formData.ec1Phone) {
      newErrors.ec1Phone = 'Vui lòng nhập số điện thoại';
    } else if (!isVnPhone(formData.ec1Phone)) {
      newErrors.ec1Phone = 'Số điện thoại không hợp lệ';
    }

    if (!formData.ec2Name) newErrors.ec2Name = 'Vui lòng nhập tên người liên hệ 2';
    if (!formData.ec2Relation) newErrors.ec2Relation = 'Vui lòng chọn quan hệ';

    if (!formData.ec2Phone) {
      newErrors.ec2Phone = 'Vui lòng nhập số điện thoại';
    } else if (!isVnPhone(formData.ec2Phone)) {
      newErrors.ec2Phone = 'Số điện thoại không hợp lệ';
    }

    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }
    setErrors({});
    setSubView('CONFIRM');
  };

  // ---- APPLY VIEW ----
  if (subView === 'APPLY') return (
    <div className="min-h-[calc(100vh-3.5rem)]">
      <div className="bg-white border-b border-slate-200 px-4 sm:px-6 lg:px-8 py-4">
        <div className="max-w-4xl mx-auto flex items-center gap-3">
          <button onClick={() => setCurrentView('HOME')} className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition cursor-pointer">
            <ChevronLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg font-bold text-slate-800">Đăng ký khoản vay</h1>
            <p className="text-xs text-slate-500">Điền đầy đủ thông tin bên dưới</p>
          </div>
        </div>
      </div>

      <div className="py-3 px-4 lg:px-8 max-w-6xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

          {/* LEFT COLUMN */}
          <div className="space-y-3">
            {/* Loan needs */}
            <section className="bg-white border border-slate-200 rounded-xl overflow-hidden h-fit">
              <div className="px-4 py-2 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">Thông tin nhu cầu</h2>
              </div>
              <div className="p-4 space-y-3">
                <div>
                  <div className="flex items-end justify-between mb-1">
                    <p className="text-xs font-medium text-slate-600">Bạn muốn vay</p>
                    <p className="text-xl font-extrabold text-slate-900 tracking-tight">{formatAmount(loanMillions)} VNĐ</p>
                  </div>
                  <input
                    type="range" min={0} max={16} step={1} value={loanSlider}
                    onChange={e => handleSliderChange(Number(e.target.value))}
                    className="loan-slider w-full h-1.5 rounded-full appearance-none cursor-pointer mt-1"
                    style={{ background: `linear-gradient(to right, #2563eb ${(loanSlider / 16) * 100}%, #bfdbfe ${(loanSlider / 16) * 100}%)` }}
                  />
                  <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-medium">
                    <span>3 triệu</span><span>100 triệu</span>
                  </div>
                  {errors.amount && <p className="text-red-500 text-[10px] mt-1 font-medium">{errors.amount}</p>}
                </div>
                <div>
                  <p className="text-xs font-medium text-slate-600 mb-1.5">Kỳ hạn vay <span className="text-red-500">*</span></p>
                  <div className="grid grid-cols-3 sm:grid-cols-4 gap-1.5">
                    {availableTerms.map(t => (
                      <button key={t} type="button"
                        onClick={() => { handleChange('term', String(t)); }}
                        className={`py-1.5 px-2 rounded-lg text-xs font-semibold border-2 transition cursor-pointer ${formData.term === String(t) ? 'border-blue-600 bg-blue-600 text-white' : 'border-slate-200 bg-white text-slate-700 hover:border-blue-400 hover:text-blue-600'}`}
                      >
                        {t} tháng
                      </button>
                    ))}
                  </div>
                  {errors.term && <p className="text-red-500 text-[10px] mt-1 font-medium">{errors.term}</p>}
                </div>
                <div>
                  <label className="text-xs font-medium text-slate-700 mb-1 block">Mục đích vay <span className="text-red-500">*</span></label>
                  <select value={formData.purpose} onChange={e => handleChange('purpose', e.target.value)} className={inputCls('purpose')}>
                    <option value="">Chọn mục đích</option>
                    <option>Mua phương tiện đi lại</option>
                    <option>Mua sắm đồ dùng sinh hoạt gia đình</option>
                    <option>Học tập</option>
                    <option>Chữa bệnh</option>
                    <option>Du lịch</option>
                    <option>Tiêu dùng khác</option>
                  </select>
                  {errors.purpose && <p className="text-red-500 text-[10px] mt-1 font-medium">{errors.purpose}</p>}
                </div>
                <div className="bg-blue-50/50 border border-blue-100 rounded-lg p-3 mt-1">
                  <div className="flex justify-between items-center text-xs text-blue-800 mb-1.5">
                    <span>Lãi suất (cố định)</span>
                    <span className="font-semibold">{interestRate}%/tháng</span>
                  </div>
                  <div className="flex justify-between items-center text-xs text-blue-800">
                    <span>Tạm tính trả/tháng</span>
                    <span className="font-bold">{formattedPayment}</span>
                  </div>
                </div>
              </div>
            </section>

            {/* Emergency contacts */}
            <section className="bg-white border border-slate-200 rounded-xl overflow-hidden h-fit">
              <div className="px-4 py-2 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">Người liên hệ khẩn cấp</h2>
              </div>
              <div className="p-4 space-y-3">
                {[1, 2].map(n => {
                  const prefix = n === 1 ? 'ec1' : 'ec2';
                  const nameKey = `${prefix}Name` as keyof FormData;
                  const relKey = `${prefix}Relation` as keyof FormData;
                  const phoneKey = `${prefix}Phone` as keyof FormData;
                  return (
                    <div key={n}>
                      {n === 2 && <div className="border-t border-slate-100 mb-3" />}
                      <p className="text-[10px] font-semibold text-slate-500 uppercase tracking-wide mb-1.5">Người liên hệ {n} <span className="text-red-500">*</span></p>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                        <div className="sm:col-span-3">
                          <input type="text" placeholder="Họ và tên" value={formData[nameKey]} onChange={e => handleChange(nameKey, e.target.value)} className={inputCls(nameKey)} />
                          {errors[nameKey] && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors[nameKey]}</p>}
                        </div>
                        <div className="sm:col-span-1">
                          <select value={formData[relKey]} onChange={e => handleChange(relKey, e.target.value)} className={inputCls(relKey)}>
                            <option value="">Quan hệ</option>
                            <option>Bố/Mẹ</option>
                            <option>Vợ/Chồng</option>
                            <option>Anh/Chị/Em</option>
                            <option>Bạn bè/Đồng nghiệp</option>
                          </select>
                          {errors[relKey] && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors[relKey]}</p>}
                        </div>
                        <div className="sm:col-span-2">
                          <input type="tel" placeholder="Số điện thoại" value={formData[phoneKey]} onChange={e => handleChange(phoneKey, e.target.value)} className={inputCls(phoneKey)} />
                          {errors[phoneKey] && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors[phoneKey]}</p>}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          </div>

          {/* RIGHT COLUMN */}
          <div className="space-y-3">
            {/* Personal info */}
            <section className="bg-white border border-slate-200 rounded-xl overflow-hidden h-fit">
              <div className="px-4 py-2 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">Thông tin cá nhân</h2>
              </div>
              <div className="p-4 space-y-4">
                <div className="space-y-2.5">
                  <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-200 pb-2">
                    <span className="text-slate-500">Họ và tên</span>
                    <span className="font-semibold text-slate-800">{formData.fullName}</span>
                  </div>
                  <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-200 pb-2">
                    <span className="text-slate-500">Ngày sinh</span>
                    <span className="font-medium text-slate-800">{formatDate(formData.dob)}</span>
                  </div>
                  <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-200 pb-2">
                    <span className="text-slate-500">CCCD/CMND</span>
                    <span className="font-medium text-slate-800">{formData.cccd}</span>
                  </div>
                  <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-200 pb-2">
                    <span className="text-slate-500">Số điện thoại</span>
                    <span className="font-medium text-slate-800">{formData.phone}</span>
                  </div>
                  <div className="pt-2">
                    <label className="text-xs font-medium text-slate-700 mb-1.5 block">Giới tính <span className="text-red-500">*</span></label>
                    <div className="flex gap-6">
                      <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                        <input type="radio" name="gender" checked={formData.gender === 'Nam'} onChange={() => handleChange('gender', 'Nam')} className="w-4 h-4 accent-blue-600" /> Nam
                      </label>
                      <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                        <input type="radio" name="gender" checked={formData.gender === 'Nữ'} onChange={() => handleChange('gender', 'Nữ')} className="w-4 h-4 accent-blue-600" /> Nữ
                      </label>
                    </div>
                  </div>
                  <div className="pt-1">
                    <label className="text-xs font-medium text-slate-700 mb-1 block">Email <span className="text-red-500">*</span></label>
                    <input type="email" placeholder="Nhập email" value={formData.email} onChange={e => handleChange('email', e.target.value)} className={inputCls('email')} />
                    {errors.email && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.email}</p>}
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-slate-100">
                  <h3 className="text-xs font-semibold text-slate-700 mb-2">Địa chỉ</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="text-[10px] font-medium text-slate-500 mb-0.5 block">Tỉnh/Thành phố <span className="text-red-500">*</span></label>
                      <input type="text" placeholder="" value={formData.city} onChange={e => handleChange('city', e.target.value)} className={inputCls('city')} />
                      {errors.city && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.city}</p>}
                    </div>
                    <div>
                      <label className="text-[10px] font-medium text-slate-500 mb-0.5 block">Xã/Phường <span className="text-red-500">*</span></label>
                      <input type="text" placeholder="" value={formData.ward} onChange={e => handleChange('ward', e.target.value)} className={inputCls('ward')} />
                      {errors.ward && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.ward}</p>}
                    </div>
                    <div className="sm:col-span-2">
                      <label className="text-[10px] font-medium text-slate-500 mb-0.5 block">Số nhà, tên đường <span className="text-red-500">*</span></label>
                      <input type="text" placeholder="" value={formData.address} onChange={e => handleChange('address', e.target.value)} className={inputCls('address')} />
                      {errors.address && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.address}</p>}
                    </div>
                  </div>
                </div>
              </div>
            </section>

            {/* Occupation */}
            <section className="bg-white border border-slate-200 rounded-xl overflow-hidden h-fit">
              <div className="px-4 py-2 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">Thông tin nghề nghiệp</h2>
              </div>
              <div className="p-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-slate-700 mb-1 block">Nghề nghiệp <span className="text-red-500">*</span></label>
                  <select value={formData.occupation} onChange={e => handleChange('occupation', e.target.value)} className={inputCls('occupation')}>
                    <option value="">Chọn nghề nghiệp</option>
                    <option>Cán bộ / Công chức</option>
                    <option>Hưu trí</option>
                    <option>Sinh viên</option>
                    <option>Kinh doanh tự do</option>
                    <option>Nhân viên công ty</option>
                    <option>Khác</option>
                  </select>
                  {errors.occupation && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.occupation}</p>}
                </div>
                <div>
                  <label className="text-xs font-medium text-slate-700 mb-1 block">Thu nhập hằng tháng <span className="text-red-500">*</span></label>
                  <select value={formData.income} onChange={e => handleChange('income', e.target.value)} className={inputCls('income')}>
                    <option value="">Chọn mức thu nhập</option>
                    <option>Dưới 10 triệu</option>
                    <option>10 - 20 triệu</option>
                    <option>20 - 50 triệu</option>
                    <option>50 - 100 triệu</option>
                    <option>Trên 100 triệu</option>
                  </select>
                  {errors.income && <p className="text-red-500 text-[10px] mt-0.5 font-medium">{errors.income}</p>}
                </div>
              </div>
            </section>
          </div>
        </div>

        <div className="flex justify-between items-center mt-4">
          <button
            onClick={() => {
              const draftKey = `draft_application_${formData.email}`;
              const draftData = { ...formData, draftDate: new Date().toLocaleDateString('vi-VN') };
              localStorage.setItem(draftKey, JSON.stringify(draftData));
              alert('Đã lưu nháp hồ sơ thành công.');
              setCurrentView('HOME');
            }}
            className="inline-flex items-center gap-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold px-6 py-2 rounded-lg shadow-sm transition cursor-pointer text-[13px]"
          >
            Lưu nháp
          </button>
          <button onClick={handleProceed} className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-2 rounded-lg shadow-sm transition cursor-pointer text-[13px]">
            Tiếp tục <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );

  // ---- CONFIRM VIEW ----
  if (subView === 'CONFIRM') {
    return (
      <div className="min-h-[calc(100vh-3.5rem)]">
        <div className="bg-white border-b border-slate-200 px-4 sm:px-6 lg:px-8 py-4">
          <div className="max-w-4xl mx-auto flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button onClick={() => setSubView('APPLY')} className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition cursor-pointer">
                <ChevronLeft className="w-5 h-5" />
              </button>
              <div>
                <h1 className="text-lg font-bold text-slate-800">Xác nhận thông tin</h1>
                <p className="text-xs text-slate-500">Kiểm tra kỹ trước khi gửi hồ sơ</p>
              </div>
            </div>
            <button onClick={() => setSubView('APPLY')} className="inline-flex items-center gap-1.5 text-sm font-medium text-blue-600 hover:bg-blue-50 px-3 py-1.5 rounded-lg transition cursor-pointer">
              <Edit3 className="w-3.5 h-3.5" /> Chỉnh sửa
            </button>
          </div>
        </div>

        <div className="py-6 sm:py-8 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto space-y-5">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">1. Thông tin khoản vay</h2>
              </div>
              <div className="p-5 space-y-3">
                <div className="flex justify-between text-sm"><span className="text-slate-500">Số tiền đề nghị</span><span className="font-semibold text-slate-800">{formData.amount.replace(/\s*(VND|VNĐ)/gi, '')} VNĐ</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Thời gian vay</span><span className="font-semibold text-slate-800">{formData.term} tháng</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Lãi suất</span><span className="font-semibold text-slate-800">{interestRate}%/tháng</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Mục đích</span><span className="font-medium text-slate-800 text-right max-w-[60%]">{formData.purpose}</span></div>
                <div className="border-t border-slate-100 pt-3 flex justify-between text-sm"><span className="font-medium text-slate-600">Tạm tính trả/tháng</span><span className="font-bold text-blue-600">~{formattedPayment}</span></div>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">2. Thông tin cá nhân</h2>
              </div>
              <div className="p-5 space-y-3">
                <div className="flex justify-between text-sm"><span className="text-slate-500">Họ và tên</span><span className="font-semibold text-slate-800">{formData.fullName}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Ngày sinh</span><span className="font-medium text-slate-800">{formatDate(formData.dob)}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Giới tính</span><span className="font-medium text-slate-800">{formData.gender}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">CCCD/CMND</span><span className="font-medium text-slate-800">{formData.cccd}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Số điện thoại</span><span className="font-medium text-slate-800">{formData.phone}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Email</span><span className="font-medium text-slate-800 truncate max-w-[60%]">{formData.email}</span></div>
                <div className="flex justify-between text-sm items-start"><span className="text-slate-500 mr-4">Địa chỉ</span><span className="font-medium text-slate-800 text-right">{formData.address}, {formData.ward}, {formData.city}</span></div>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">3. Thu nhập & Công việc</h2>
              </div>
              <div className="p-5 space-y-3">
                <div className="flex justify-between text-sm"><span className="text-slate-500">Nghề nghiệp</span><span className="font-medium text-slate-800">{formData.occupation}</span></div>
                <div className="flex justify-between text-sm"><span className="text-slate-500">Thu nhập</span><span className="font-medium text-emerald-600">{formData.income}</span></div>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
                <h2 className="font-semibold text-slate-800 text-sm">4. Người liên hệ khẩn cấp</h2>
              </div>
              <div className="p-5 space-y-4">
                <div>
                  <p className="text-xs text-slate-400 font-medium uppercase mb-1">Người thứ 1 ({formData.ec1Relation || 'Chưa chọn'})</p>
                  <div className="flex justify-between text-sm"><span className="font-medium text-slate-800">{formData.ec1Name || '—'}</span><span className="font-semibold text-slate-700">{formData.ec1Phone || '—'}</span></div>
                </div>
                <div className="border-t border-slate-100 pt-3">
                  <p className="text-xs text-slate-400 font-medium uppercase mb-1">Người thứ 2 ({formData.ec2Relation || 'Chưa chọn'})</p>
                  <div className="flex justify-between text-sm"><span className="font-medium text-slate-800">{formData.ec2Name || '—'}</span><span className="font-semibold text-slate-700">{formData.ec2Phone || '—'}</span></div>
                </div>
              </div>
            </div>
          </div>

          {/* Contract section */}
          <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center">
                  <FileText className="w-4 h-4 text-blue-600" />
                </div>
                <div>
                  <h2 className="font-semibold text-slate-800 text-sm">5. Hợp đồng tín dụng</h2>
                  <p className="text-xs text-slate-400 mt-0.5">Mẫu hợp đồng được tạo tự động theo thông tin đăng ký</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-blue-600 hover:bg-blue-50 px-3 py-1.5 rounded-lg border border-slate-200 hover:border-blue-200 transition cursor-pointer">
                  <Eye className="w-3.5 h-3.5" /> Xem toàn văn
                </button>
                <button disabled={isGeneratingPDF} onClick={handleDownloadPDF} className="inline-flex items-center gap-1.5 text-xs font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 px-3 py-1.5 rounded-lg transition cursor-pointer">
                  {isGeneratingPDF ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                  {isGeneratingPDF ? 'Đang tạo PDF...' : 'Tải PDF'}
                </button>
              </div>
            </div>
            <div className="p-5 sm:p-6">
              <ContractDocument formData={formData} interestRate={interestRate} />
            </div>
          </div>

          {/* Agreement */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-5">
            <label className="flex items-start gap-3 cursor-pointer">
              <input type="checkbox" className="w-4 h-4 mt-0.5 accent-blue-600 rounded" checked={isAgreed} onChange={e => setIsAgreed(e.target.checked)} />
              <span className="text-sm text-slate-600 leading-relaxed">
                Tôi cam kết các thông tin trên là chính xác. Tôi đã đọc và đồng ý với <b className="text-slate-800">Điều khoản cấp tín dụng</b>, chấp thuận sử dụng chữ ký điện tử để nộp hồ sơ.
              </span>
            </label>
          </div>

          <div className="flex justify-end mt-4">
            <button
              onClick={async () => {
                setIsSendingOTP(true);
                try {
                  await sendOtpAPI(formData.email);
                  setSubView('OTP');
                } catch (e: any) {
                  alert(e.message || 'Có lỗi khi gửi OTP');
                } finally {
                  setIsSendingOTP(false);
                }
              }}
              disabled={!isAgreed || isSendingOTP}
              className={`inline-flex items-center gap-2 font-semibold px-8 py-2.5 rounded-lg text-sm transition ${isAgreed && !isSendingOTP ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm cursor-pointer' : 'bg-slate-200 text-slate-400 cursor-not-allowed'}`}
            >
              {isSendingOTP ? 'Đang gửi mã...' : 'Ký Hợp Đồng & Gửi Duyệt'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ---- OTP VIEW ----
  if (subView === 'OTP') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-3.5rem)] px-4 py-8 bg-[#f4f6f9]">
        <div className="bg-white p-8 rounded shadow-[0_2px_4px_rgba(0,0,0,0.1)] border border-[#dddbda] max-w-[400px] w-full text-left">

          <h2 className="text-[1.5rem] font-light text-[#16325c] mb-4">Xác thực danh tính</h2>

          <p className="text-[#16325c] text-[13px] leading-relaxed mb-6">
            Bạn đang thực hiện ký Hợp đồng vay. Để đảm bảo tính bảo mật, chúng tôi cần xác minh danh tính của bạn.
            Chúng tôi đã gửi mã xác thực gồm 6 chữ số đến <strong>{formData.email}</strong>.
          </p>

          <div className="mb-6">
            <label className="block text-[12px] text-[#3e3e3c] font-bold mb-1.5">
              <span className="text-[#ea001e] mr-0.5">*</span>
              Mã xác thực
            </label>
            <input
              type="text"
              maxLength={6}
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
              className="w-full border border-[#dddbda] rounded-[4px] px-3 py-2 text-[#16325c] focus:outline-none focus:border-[#1b96ff] focus:shadow-[0_0_3px_#0176d3] transition-shadow text-sm"
            />
          </div>

          <button
            onClick={async () => {
              if (otpCode.length < 6) {
                setApiError('Vui lòng nhập đủ mã xác thực 6 chữ số');
                return;
              }
              setIsSubmitting(true);
              setApiError('');
              try {
                // 1. Verify OTP
                await verifyOtpAPI(formData.email, otpCode);

                // 2. Submit Loan Application
                const amountNum = Number(String(formData.amount || '').replace(/\D/g, '')) || (sliderToMillions(loanSlider) * 1_000_000);
                const termNum = Number(String(formData.term || '').replace(/\D/g, '')) || 12;
                await submitLoanAPI({
                  amount: amountNum,
                  term: termNum,
                  fullName: formData.fullName,
                  cccd: formData.cccd,
                  email: formData.email,
                  phoneNumber: formData.phone,
                  gender: formData.gender,
                  birthDate: formData.dob,
                  address: [formData.address, formData.ward, formData.district, formData.city].filter(Boolean).join(', '),
                  occupation: formData.occupation,
                  incomeRange: formData.income,
                  purpose: formData.purpose,
                  ref1Name: formData.ec1Name,
                  ref1Phone: formData.ec1Phone,
                  ref2Name: formData.ec2Name,
                  ref2Phone: formData.ec2Phone,
                });

                const draftKey = `draft_application_${formData.email}`;
                localStorage.removeItem(draftKey);
                setSubView('SUCCESS');
              } catch (error: any) {
                setApiError(error.message || 'Không thể kết nối đến máy chủ');
              } finally {
                setIsSubmitting(false);
              }
            }}
            disabled={isSubmitting}
            className={`w-full font-normal py-2 rounded-[4px] transition text-sm mb-4 ${isSubmitting ? 'bg-[#c9c7c5] text-white cursor-not-allowed' : 'bg-[#0176d3] hover:bg-[#014486] text-white cursor-pointer'}`}
          >
            {isSubmitting ? 'Đang xác thực...' : 'Xác minh'}
          </button>

          {apiError && (
            <div className="bg-[#fef2f2] border border-[#fca5a5] text-[#7f1d1d] text-[13px] px-3 py-2 rounded-[4px] mb-4">
              {apiError}
            </div>
          )}

          <p className="text-[13px] text-center text-[#16325c]">
            Không nhận được mã? <button className="text-[#0176d3] hover:underline cursor-pointer"
              onClick={async () => {
                setIsSendingOTP(true);
                setApiError('');
                try {
                  await sendOtpAPI(formData.email);
                  alert('Đã gửi lại mã OTP thành công!');
                } catch (error: any) {
                  setApiError(error.message || 'Không thể kết nối đến máy chủ');
                } finally {
                  setIsSendingOTP(false);
                }
              }}>
              Gửi lại mã</button>
          </p>
        </div>
      </div>
    );
  }

  // ---- SUCCESS VIEW ----
  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-3.5rem)] px-6">
      <div className="max-w-md text-center">
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-5">
          <CheckCircle2 className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-800 mb-2">Hồ sơ đã được gửi thành công</h2>
        <p className="text-slate-500 text-sm leading-relaxed mb-6">
          Hồ sơ vay của bạn đã được khởi tạo và chuyển vào hệ thống để phê duyệt.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button onClick={() => setCurrentView('HISTORY')} className="inline-flex items-center justify-center gap-2 px-5 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition cursor-pointer">
            Xem hồ sơ
          </button>
          <button onClick={() => { setIsAgreed(false); setSubView('APPLY'); setCurrentView('HOME'); }} className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-blue-600 rounded-lg text-sm font-semibold text-white hover:bg-blue-700 transition cursor-pointer">
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
  );
};

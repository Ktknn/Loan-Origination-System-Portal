import { useState, useRef, useEffect } from 'react';
import { Search, CreditCard, ArrowRight, FileText, Clock, CheckCircle2, XCircle, AlertCircle, ChevronRight, Calendar, X, CalendarDays } from 'lucide-react';
import { ViewType, FormData } from './shared/types';
import { DatePickerPopup } from './shared/DatePickerPopup';
import { getLoanHistoryAPI } from '../../services/api';

interface LookupPageProps {
  formData: FormData;
  setCurrentView: (v: ViewType) => void;
}

// =============================================
// MOCK DATA
// =============================================
interface LoanRecord {
  id: string;
  code: string;
  amount: string;
  term: number;
  purpose: string;
  submittedDate: string;
  submittedDateTime?: string;
  status: 'pending' | 'reviewing' | 'approved' | 'rejected';
  approvedInterestRate: number;
}

const STATUS_CONFIG = {
  pending: {
    label: 'Chờ xử lý',
    icon: Clock,
    cls: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500',
  },
  reviewing: {
    label: 'Đang thẩm định',
    icon: AlertCircle,
    cls: 'bg-blue-50 text-blue-700 border-blue-200',
    dot: 'bg-blue-500',
  },
  approved: {
    label: 'Đã duyệt',
    icon: CheckCircle2,
    cls: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
  },
  rejected: {
    label: 'Từ chối',
    icon: XCircle,
    cls: 'bg-red-50 text-red-700 border-red-200',
    dot: 'bg-red-500',
  },
};


// =============================================
// HELPERS
// =============================================
const toDateStr = (d: Date) => d.toISOString().split('T')[0];

const defaultTo = toDateStr(new Date());
const defaultFrom = (() => {
  const d = new Date();
  d.setMonth(d.getMonth() - 1);
  return toDateStr(d);
})();

const formatDateVN = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const formatTimeVN = (iso?: string) => {
  if (!iso) return '00:00';
  try {
    const d = new Date(iso);
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch {
    return '00:00';
  }
};

// =============================================
// LOOKUP PAGE
// =============================================
export const LookupPage = ({ formData, setCurrentView }: LookupPageProps) => {
  const [fromDate, setFromDate] = useState(defaultFrom);
  const [toDate, setToDate] = useState(defaultTo);
  const [searched, setSearched] = useState(false);
  const [results, setResults] = useState<LoanRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [dateError, setDateError] = useState('');
  const [selectedRecord, setSelectedRecord] = useState<LoanRecord | null>(null);

  const [showFromPicker, setShowFromPicker] = useState(false);
  const [showToPicker, setShowToPicker] = useState(false);
  const fromRef = useRef<HTMLDivElement>(null);
  const toRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (fromRef.current && !fromRef.current.contains(e.target as Node)) setShowFromPicker(false);
      if (toRef.current && !toRef.current.contains(e.target as Node)) setShowToPicker(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const isoToDate = (iso: string) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };
  const dateToISO = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  const fmtBtn = (iso: string) => { const [y,m,d] = iso.split('-'); return `${d}/${m}/${y}`; };

  const handleSearch = async () => {
    if (!fromDate || !toDate) {
      setDateError('Vui lòng chọn đầy đủ khoảng ngày.');
      return;
    }
    if (fromDate > toDate) {
      setDateError('Ngày bắt đầu phải nhỏ hơn ngày kết thúc.');
      return;
    }
    setDateError('');
    setLoading(true);

    try {
      const records = await getLoanHistoryAPI(formData.cccd, fromDate, toDate);

      const mappedRecords: LoanRecord[] = records.map((r: any) => {
        let st = 'pending';
        if (r.status === 'SUBMITTED') st = 'reviewing';
        if (r.status === 'APPROVED') st = 'approved';
        if (r.status === 'REJECTED') st = 'rejected';

        const submittedIso = r.submittedAt ? String(r.submittedAt).split('T')[0] : toDateStr(new Date());
        const displayCode = r.loanApplicationId
          ? 'LOAN-' + r.loanApplicationId.replace(/-/g, '').substring(0, 8).toUpperCase()
          : 'LOAN';

        return {
          id: r.loanApplicationId,
          code: displayCode,
          amount: r.amount ? new Intl.NumberFormat('vi-VN').format(r.amount) + ' đ' : '0 đ',
          term: r.term || 0,
          purpose: r.purpose,
          submittedDate: submittedIso,
          submittedDateTime: r.submittedAt || new Date().toISOString(),
          status: st as any,
          approvedInterestRate: r.interestRate || 0,
        };
      });

      setResults(mappedRecords);
    } catch (error: any) {
      setDateError(error.message || 'Lỗi kết nối.');
    } finally {
      setSearched(true);
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFromDate(defaultFrom);
    setToDate(defaultTo);
    setSearched(false);
    setResults([]);
    setDateError('');
  };

  return (
    <div className="py-6 sm:py-8 px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto space-y-6">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-800">Tra cứu hồ sơ vay</h1>
          <p className="text-slate-500 text-sm mt-0.5">Tìm kiếm hồ sơ theo khoảng thời gian nộp</p>
        </div>
        <button
          onClick={() => setCurrentView('APPLY')}
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold px-5 py-2.5 rounded-lg shadow-sm transition cursor-pointer text-sm self-start"
        >
          <CreditCard className="w-4 h-4" />
          Vay mới
        </button>
      </div>

      {/* Search panel */}
      <div className="bg-white border border-slate-200 rounded-xl p-5 sm:p-6">
        <div className="flex items-center gap-2 mb-4">
          <Calendar className="w-4 h-4 text-blue-600" />
          <h2 className="font-semibold text-slate-800 text-sm">Khoảng thời gian tra cứu</h2>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 items-end">
          <div className="flex-1">
            <label className="text-xs font-medium text-slate-500 mb-1.5 block">Từ ngày</label>
            <div ref={fromRef} className="relative">
              <button
                type="button"
                onClick={() => { setShowFromPicker(v => !v); setShowToPicker(false); }}
                className="w-full flex items-center justify-between border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition bg-white cursor-pointer hover:border-blue-400"
              >
                <span>{fmtBtn(fromDate)}</span>
                <CalendarDays className="w-4 h-4 text-slate-400" />
              </button>
              {showFromPicker && (
                <div className="absolute z-50 mt-1">
                  <DatePickerPopup
                    value={isoToDate(fromDate)}
                    onChange={(d) => { setFromDate(dateToISO(d)); setDateError(''); }}
                    onClose={() => setShowFromPicker(false)}
                  />
                </div>
              )}
            </div>
          </div>

          <div className="hidden sm:flex items-center pb-2.5 text-slate-400 text-sm font-medium">→</div>

          <div className="flex-1">
            <label className="text-xs font-medium text-slate-500 mb-1.5 block">Đến ngày</label>
            <div ref={toRef} className="relative">
              <button
                type="button"
                onClick={() => { setShowToPicker(v => !v); setShowFromPicker(false); }}
                className="w-full flex items-center justify-between border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition bg-white cursor-pointer hover:border-blue-400"
              >
                <span>{fmtBtn(toDate)}</span>
                <CalendarDays className="w-4 h-4 text-slate-400" />
              </button>
              {showToPicker && (
                <div className="absolute z-50 mt-1">
                  <DatePickerPopup
                    value={isoToDate(toDate)}
                    onChange={(d) => { setToDate(dateToISO(d)); setDateError(''); }}
                    onClose={() => setShowToPicker(false)}
                  />
                </div>
              )}
            </div>
          </div>

          <div className="flex gap-2 sm:pb-0">
            <button
              onClick={handleSearch}
              disabled={loading}
              className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold px-5 py-2.5 rounded-lg transition cursor-pointer text-sm whitespace-nowrap"
            >
              {loading
                ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                : <Search className="w-4 h-4" />}
              {loading ? 'Đang tìm...' : 'Tra cứu'}
            </button>
            {searched && (
              <button
                onClick={handleReset}
                className="px-4 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-100 border border-slate-200 rounded-lg transition cursor-pointer whitespace-nowrap"
              >
                Đặt lại
              </button>
            )}
          </div>
        </div>

        {dateError && (
          <p className="text-red-500 text-xs mt-2 font-medium">{dateError}</p>
        )}

        {/* Quick range chips */}
        <div className="flex flex-wrap gap-2 mt-4">
          {[
            { label: '7 ngày qua', days: 7 },
            { label: '1 tháng qua', days: 30 },
            { label: '3 tháng qua', days: 90 },
            { label: '6 tháng qua', days: 180 },
            { label: '1 năm qua', days: 365 },
          ].map(({ label, days }) => (
            <button
              key={days}
              onClick={() => {
                const to = new Date();
                const from = new Date();
                from.setDate(from.getDate() - days);
                setToDate(toDateStr(to));
                setFromDate(toDateStr(from));
                setDateError('');
              }}
              className="px-3 py-1 text-xs font-medium text-slate-600 bg-slate-100 hover:bg-blue-50 hover:text-blue-700 rounded-full transition cursor-pointer"
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Results */}
      {searched && !loading && (
        <>
          {results.length === 0 ? (
            <div className="bg-white border border-slate-200 rounded-xl p-12 text-center">
              <Search className="w-12 h-12 text-slate-300 mx-auto mb-4" />
              <h2 className="font-semibold text-slate-700 text-base">Không tìm thấy hồ sơ</h2>
              <p className="text-slate-500 text-sm mt-1.5 max-w-sm mx-auto">
                Không có hồ sơ nào được nộp trong khoảng{' '}
                <span className="font-medium text-slate-700">{formatDateVN(fromDate)}</span>
                {' '}–{' '}
                <span className="font-medium text-slate-700">{formatDateVN(toDate)}</span>.
              </p>
              <button
                onClick={() => setCurrentView('APPLY')}
                className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-blue-600 hover:bg-blue-50 px-4 py-2 rounded-lg transition cursor-pointer"
              >
                Đăng ký vay ngay <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-600">
                  Tìm thấy <span className="font-bold text-slate-800">{results.length}</span> hồ sơ
                  {' '}từ <span className="font-medium">{formatDateVN(fromDate)}</span>
                  {' '}đến <span className="font-medium">{formatDateVN(toDate)}</span>
                </p>
              </div>

              {results.map(record => {
                const status = STATUS_CONFIG[record.status];
                const StatusIcon = status.icon;
                return (
                  <div
                    key={record.id}
                    onClick={() => setSelectedRecord(record)}
                    className="bg-white border border-slate-200 rounded-xl p-5 hover:border-blue-200 hover:shadow-sm transition cursor-pointer group"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex items-start gap-4">
                        <div className="w-10 h-10 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0 group-hover:bg-blue-100 transition">
                          <FileText className="w-5 h-5 text-blue-600" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2 flex-wrap">
                            <p className="font-semibold text-slate-800 text-sm">{record.code}</p>
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${status.cls}`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${status.dot}`} />
                              {status.label}
                            </span>
                          </div>
                          <p className="text-xs text-slate-500 mt-0.5">
                            Nộp ngày {formatDateVN(record.submittedDate)}
                          </p>
                          <p className="text-xs text-slate-500 mt-0.5">{record.purpose}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-6 sm:gap-8 pl-14 sm:pl-0">
                        <div className="text-right">
                          <p className="text-xs text-slate-400">Số tiền</p>
                          <p className="font-bold text-slate-800 text-sm">{record.amount}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-xs text-slate-400">Kỳ hạn</p>
                          <p className="font-semibold text-slate-700 text-sm">{record.term} tháng</p>
                        </div>
                        <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600 transition flex-shrink-0" />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {/* Initial hint — before first search */}
      {!searched && !loading && (
        <div className="bg-slate-50 border border-dashed border-slate-300 rounded-xl p-8 text-center">
          <Search className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 text-sm">
            Chọn khoảng thời gian và nhấn <span className="font-semibold text-slate-700">Tra cứu</span> để xem danh sách hồ sơ.
          </p>
          <p className="text-slate-400 text-xs mt-1">Mặc định: 1 tháng gần nhất</p>
        </div>
      )}

      {/* Centered Modal Detail Panel */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
            onClick={() => setSelectedRecord(null)}
          />

          {/* Minimalist Panel */}
          <div className="relative w-full max-w-5xl bg-white rounded shadow-xl flex flex-col max-h-[90vh] overflow-hidden animate-in zoom-in-95 duration-200 border border-slate-200">

            {/* Header Minimalist */}
            <div className="flex items-center justify-between px-8 py-5 border-b border-slate-100">
              <div>
                <h2 className="text-xl font-light text-slate-800">Hồ sơ vay: <span className="font-semibold">{selectedRecord.code}</span></h2>
                <p className="text-sm text-slate-500 mt-1">Ngày nộp: {formatDateVN(selectedRecord.submittedDate)} • Trạng thái: {STATUS_CONFIG[selectedRecord.status].label}</p>
              </div>
              <button
                onClick={() => setSelectedRecord(null)}
                className="p-2 text-slate-400 hover:text-slate-800 transition cursor-pointer"
              >
                <X className="w-6 h-6 stroke-[1.5]" />
              </button>
            </div>

            {/* Content Body: 2 Columns */}
            <div className="flex-1 overflow-y-auto p-8 flex flex-col sm:flex-row gap-12 bg-white">

              {/* Left Pane: Dense Details */}
              <div className="flex-1 space-y-6">
                <h3 className="font-medium uppercase tracking-widest text-xs mb-4 text-slate-400">Thông tin chi tiết</h3>

                <div className="grid grid-cols-2 gap-y-4 gap-x-8 text-sm">
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Người đứng tên</p>
                    <p className="font-medium text-slate-900">{formData.fullName}</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Số tiền đề nghị</p>
                    <p className="font-medium text-slate-900 text-lg">{selectedRecord.amount}</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Mục đích vay</p>
                    <p className="font-medium text-slate-900">{selectedRecord.purpose}</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Kỳ hạn</p>
                    <p className="font-medium text-slate-900">{selectedRecord.term} tháng</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Lãi suất áp dụng</p>
                    <p className="font-medium text-slate-900">{selectedRecord.approvedInterestRate}% / tháng</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Tạm tính trả hàng tháng</p>
                    <p className="font-medium text-slate-900">~ {Math.round(parseInt(selectedRecord.amount.replace(/\D/g, '')) / selectedRecord.term + parseInt(selectedRecord.amount.replace(/\D/g, '')) * (selectedRecord.approvedInterestRate / 100)).toLocaleString('vi-VN')} đ</p>
                  </div>
                  <div className="border-b border-slate-50 pb-2">
                    <p className="text-slate-500 mb-1">Kênh tiếp nhận</p>
                    <p className="font-medium text-slate-900">Trực tuyến (Web)</p>
                  </div>
                </div>
              </div>

              {/* Right Pane: Minimalist Timeline */}
              <div className="sm:w-1/3">
                <h3 className="font-medium uppercase tracking-widest text-xs mb-6 text-slate-400">Tiến trình xử lý</h3>
                <div className="space-y-6">
                  {/* Step 1: Nộp hồ sơ (Always done) */}
                  <div className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className="w-2 h-2 rounded-full bg-slate-800 mt-1.5" />
                      <div className="w-px h-10 bg-slate-200 my-1" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-slate-900">Nộp hồ sơ thành công</p>
                      <p className="text-xs text-slate-500 mt-1">{formatTimeVN(selectedRecord.submittedDateTime)}, {formatDateVN(selectedRecord.submittedDate)}</p>
                    </div>
                  </div>

                  {/* Step 2: Tiếp nhận */}
                  <div className="flex gap-4">
                    <div className="flex flex-col items-center">
                       <div className={`w-2 h-2 rounded-full mt-1.5 ${selectedRecord.status === 'pending' ? 'bg-blue-600 ring-4 ring-blue-50 animate-pulse' : 'bg-slate-800'}`} />
                      <div className={`w-px h-10 my-1 ${selectedRecord.status === 'pending' ? 'bg-slate-100' : 'bg-slate-200'}`} />
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${selectedRecord.status === 'pending' ? 'text-blue-600' : 'text-slate-900'}`}>Tiếp nhận & Chấm điểm</p>
                      <p className={`text-xs mt-1 ${selectedRecord.status === 'pending' ? 'text-blue-400' : 'text-slate-500'}`}>{selectedRecord.status === 'pending' ? 'Giai đoạn hiện tại' : `${formatTimeVN(selectedRecord.submittedDateTime)}, ${formatDateVN(selectedRecord.submittedDate)}`}</p>
                    </div>
                  </div>

                  {/* Step 3: Thẩm định */}
                  <div className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className={`w-2 h-2 rounded-full mt-1.5 ${selectedRecord.status === 'pending' ? 'border border-slate-300' :
                        selectedRecord.status === 'reviewing' ? 'bg-blue-600 ring-4 ring-blue-50 animate-pulse' : 'bg-slate-800'
                        }`} />
                      <div className={`w-px h-10 my-1 ${['pending', 'reviewing'].includes(selectedRecord.status) ? 'bg-slate-100' : 'bg-slate-200'}`} />
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${selectedRecord.status === 'pending' ? 'text-slate-400' :
                        selectedRecord.status === 'reviewing' ? 'text-blue-600' : 'text-slate-900'
                        }`}>Thẩm định hồ sơ</p>
                      <p className={`text-xs mt-1 ${selectedRecord.status === 'pending' ? 'text-slate-400' :
                        selectedRecord.status === 'reviewing' ? 'text-blue-400' : 'text-slate-500'
                        }`}>
                        {selectedRecord.status === 'pending' ? 'Chưa diễn ra' :
                          selectedRecord.status === 'reviewing' ? 'Giai đoạn hiện tại' : 'Đã hoàn tất'}
                      </p>
                    </div>
                  </div>

                  {/* Step 4: Kết quả */}
                  <div className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className={`w-2 h-2 rounded-full mt-1.5 ${selectedRecord.status === 'approved' ? 'bg-emerald-500 ring-4 ring-emerald-50' :
                        selectedRecord.status === 'rejected' ? 'bg-red-500 ring-4 ring-red-50' : 'border border-slate-300'
                        }`} />
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${selectedRecord.status === 'approved' ? 'text-emerald-600' :
                        selectedRecord.status === 'rejected' ? 'text-red-600' : 'text-slate-400'
                        }`}>
                        {selectedRecord.status === 'rejected' ? 'Từ chối hồ sơ' : 'Phê duyệt giải ngân'}
                      </p>
                      <p className={`text-xs mt-1 ${selectedRecord.status === 'approved' ? 'text-emerald-500' :
                        selectedRecord.status === 'rejected' ? 'text-red-500' : 'text-slate-400'
                        }`}>
                        {['approved', 'rejected'].includes(selectedRecord.status) ? 'Đã có kết quả' : 'Chưa diễn ra'}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Footer Minimalist */}
            <div className="px-8 py-4 bg-slate-50 border-t border-slate-100 flex justify-end">
              <button
                className="px-6 py-2 bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium rounded transition cursor-pointer"
                onClick={() => setSelectedRecord(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

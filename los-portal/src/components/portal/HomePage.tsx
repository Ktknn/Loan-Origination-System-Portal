import { useState, useEffect } from 'react';
import { CreditCard, FileText, TrendingUp, Calendar, Clock, ArrowUpRight, Search, Headphones, AlertCircle, Trash2 } from 'lucide-react';
import { ViewType, FormData } from './shared/types';

interface HomePageProps {
  formData: FormData;
  setCurrentView: (v: ViewType) => void;
  setFormData: React.Dispatch<React.SetStateAction<FormData>>;
}

export const HomePage = ({ formData, setCurrentView, setFormData }: HomePageProps) => {
  const [draftApp, setDraftApp] = useState<FormData & { draftDate?: string } | null>(null);

  useEffect(() => {
    const draftKey = `draft_application_${formData.email}`;
    const savedDraft = localStorage.getItem(draftKey);
    if (savedDraft) {
      try {
        setDraftApp(JSON.parse(savedDraft));
      } catch (e) {
        console.error("Error parsing draft", e);
      }
    }
  }, [formData.email]);

  const handleApplyNew = () => {
    if (draftApp) {
      alert('Bạn đang có hồ sơ chưa hoàn thiện. Vui lòng hoàn thành hoặc xóa hồ sơ nháp trước khi tạo mới.');
    } else {
      setCurrentView('APPLY');
    }
  };

  const handleContinueDraft = () => {
    if (draftApp) {
      setFormData(draftApp);
      setCurrentView('APPLY');
    }
  };

  const handleDeleteDraft = () => {
    if (window.confirm('Bạn có chắc chắn muốn xóa hồ sơ nháp này?')) {
      const draftKey = `draft_application_${formData.email}`;
      localStorage.removeItem(draftKey);
      setDraftApp(null);
    }
  };

  return (
    <div className="py-6 sm:py-8 px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto space-y-6">
    {/* Welcome row */}
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 className="text-xl sm:text-2xl font-bold text-slate-800">Xin chào, {formData.fullName}</h1>
        <p className="text-slate-500 text-sm mt-0.5">Chào mừng bạn trở lại cổng thông tin khách hàng.</p>
      </div>
    </div>

    {/* Stats row */}
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div className="bg-white border border-slate-200 rounded-xl p-5 flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Hạn mức khả dụng</p>
          <p className="text-2xl font-bold text-slate-800 mt-1.5">100,000,000</p>
          <p className="text-xs text-slate-400 mt-0.5">VNĐ</p>
        </div>
        <div className="w-10 h-10 bg-emerald-50 rounded-lg flex items-center justify-center flex-shrink-0">
          <TrendingUp className="w-5 h-5 text-emerald-600" />
        </div>
      </div>
      <div className="bg-white border border-slate-200 rounded-xl p-5 flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Khoản vay hiện tại</p>
          <p className="text-2xl font-bold text-slate-800 mt-1.5">0</p>
          <p className="text-xs text-slate-400 mt-0.5">Đang hoạt động</p>
        </div>
        <div className="w-10 h-10 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
          <FileText className="w-5 h-5 text-blue-600" />
        </div>
      </div>
      <div className="bg-white border border-slate-200 rounded-xl p-5 flex items-start justify-between sm:col-span-2 lg:col-span-1">
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Kỳ thanh toán tiếp theo</p>
          <p className="text-2xl font-bold text-slate-800 mt-1.5">—</p>
          <p className="text-xs text-slate-400 mt-0.5">Chưa có lịch</p>
        </div>
        <div className="w-10 h-10 bg-amber-50 rounded-lg flex items-center justify-center flex-shrink-0">
          <Calendar className="w-5 h-5 text-amber-600" />
        </div>
      </div>
    </div>

    {/* Quick Actions */}
    <div>
      <h3 className="text-sm font-semibold text-slate-800 mb-3 uppercase tracking-wide">Tiện ích nhanh</h3>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <button
          onClick={handleApplyNew}
          className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col items-center justify-center gap-2 hover:border-blue-300 hover:shadow-md transition group cursor-pointer"
        >
          <div className="w-12 h-12 bg-blue-50 rounded-full flex items-center justify-center group-hover:bg-blue-100 transition">
            <CreditCard className="w-6 h-6 text-blue-600" />
          </div>
          <span className="text-sm font-semibold text-slate-700 text-center">Đăng ký<br />vay mới</span>
        </button>

        <button
          onClick={() => setCurrentView('HISTORY')}
          className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col items-center justify-center gap-2 hover:border-blue-300 hover:shadow-md transition group cursor-pointer"
        >
          <div className="w-12 h-12 bg-indigo-50 rounded-full flex items-center justify-center group-hover:bg-indigo-100 transition">
            <Search className="w-6 h-6 text-indigo-600" />
          </div>
          <span className="text-sm font-semibold text-slate-700 text-center">Tra cứu<br />hồ sơ</span>
        </button>

        <button
          className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col items-center justify-center gap-2 hover:border-blue-300 hover:shadow-md transition group cursor-pointer"
        >
          <div className="w-12 h-12 bg-amber-50 rounded-full flex items-center justify-center group-hover:bg-amber-100 transition">
            <Clock className="w-6 h-6 text-amber-600" />
          </div>
          <span className="text-sm font-semibold text-slate-700 text-center">Lịch sử<br />thanh toán</span>
        </button>

        <button
          className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col items-center justify-center gap-2 hover:border-blue-300 hover:shadow-md transition group cursor-pointer"
        >
          <div className="w-12 h-12 bg-emerald-50 rounded-full flex items-center justify-center group-hover:bg-emerald-100 transition">
            <Headphones className="w-6 h-6 text-emerald-600" />
          </div>
          <span className="text-sm font-semibold text-slate-700 text-center">Hỗ trợ<br />CSKH</span>
        </button>
      </div>
    </div>

    {draftApp && (
      <div className="mt-8">
        <h3 className="text-sm font-semibold text-slate-800 mb-3 uppercase tracking-wide">Hồ sơ chưa hoàn thiện</h3>
        <div className="bg-white border border-amber-200 rounded-xl p-5 shadow-sm">
          <div className="flex items-start justify-between flex-wrap gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-amber-50 rounded-lg flex items-center justify-center flex-shrink-0">
                <AlertCircle className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <p className="font-semibold text-slate-800">Đăng ký khoản vay {draftApp.amount || '...'}</p>
                <p className="text-xs text-slate-500 mt-0.5">Kỳ hạn: {draftApp.term || '...'} tháng • Lưu nháp lúc: {draftApp.draftDate || 'Gần đây'}</p>
              </div>
            </div>
            <div className="flex items-center gap-2 w-full sm:w-auto">
              <button 
                onClick={handleDeleteDraft}
                className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 px-4 py-2 border border-red-200 text-red-600 bg-red-50 hover:bg-red-100 rounded-lg text-sm font-medium transition cursor-pointer"
              >
                <Trash2 className="w-4 h-4" /> Xóa
              </button>
              <button 
                onClick={handleContinueDraft}
                className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition cursor-pointer"
              >
                Tiếp tục
              </button>
            </div>
          </div>
        </div>
      </div>
    )}
  </div>
  );
};

import { User, Calendar, Mail, MapPin, Briefcase, Settings, Lock, LogOut, ChevronRight } from 'lucide-react';
import { ViewType, FormData } from './shared/types';

interface AccountPageProps {
  formData: FormData;
  setCurrentView: (v: ViewType) => void;
}

export const AccountPage = ({ formData, setCurrentView }: AccountPageProps) => (
  <div className="py-6 sm:py-8 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto space-y-6">
    <div>
      <h1 className="text-xl font-bold text-slate-800">Tài khoản</h1>
      <p className="text-slate-500 text-sm mt-0.5">Quản lý thông tin cá nhân và cài đặt</p>
    </div>

    {/* User card */}
    <div className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col sm:flex-row items-start sm:items-center gap-4">
      <div className="w-14 h-14 bg-slate-100 rounded-full flex items-center justify-center flex-shrink-0">
        <User className="w-7 h-7 text-slate-500" />
      </div>
      <div className="flex-1">
        <h2 className="font-semibold text-slate-800 text-lg">{formData.fullName}</h2>
      </div>
    </div>

    {/* Info sections */}
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
          <h3 className="font-semibold text-slate-800 text-sm">Thông tin cá nhân</h3>
        </div>
        <div className="p-5 space-y-4">
          <div className="flex items-center gap-3 text-sm">
            <Calendar className="w-4 h-4 text-slate-400 flex-shrink-0" />
            <div>
              <p className="text-xs text-slate-400">Ngày sinh</p>
              <p className="font-medium text-slate-700">{formData.dob || 'Chưa cập nhật'}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <Mail className="w-4 h-4 text-slate-400 flex-shrink-0" />
            <div>
              <p className="text-xs text-slate-400">Email</p>
              <p className="font-medium text-slate-700">{formData.email}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/50">
          <h3 className="font-semibold text-slate-800 text-sm">Cài đặt</h3>
        </div>
        <div className="divide-y divide-slate-100">
          <div className="flex items-center justify-between px-5 py-3.5 hover:bg-slate-50 cursor-pointer transition">
            <div className="flex items-center gap-3">
              <Settings className="w-4 h-4 text-slate-400" />
              <span className="text-sm font-medium text-slate-700">Cài đặt ứng dụng</span>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400" />
          </div>
          <div className="flex items-center justify-between px-5 py-3.5 hover:bg-slate-50 cursor-pointer transition">
            <div className="flex items-center gap-3">
              <Lock className="w-4 h-4 text-slate-400" />
              <span className="text-sm font-medium text-slate-700">Đổi mật khẩu</span>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400" />
          </div>
          <div
            onClick={() => {
              localStorage.removeItem('user');
              setCurrentView('LOGIN');
            }}
            className="flex items-center justify-between px-5 py-3.5 hover:bg-red-50 cursor-pointer transition"
          >
            <div className="flex items-center gap-3">
              <LogOut className="w-4 h-4 text-red-500" />
              <span className="text-sm font-medium text-red-600">Đăng xuất</span>
            </div>
            <ChevronRight className="w-4 h-4 text-red-400" />
          </div>
        </div>
      </div>
    </div>
  </div>
);

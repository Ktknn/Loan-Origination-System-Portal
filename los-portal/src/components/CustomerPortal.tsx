import { useState, useEffect, useRef } from 'react';
import { ShieldCheck, ChevronLeft, Loader2, CalendarDays } from 'lucide-react';
import { loginAPI, registerAPI, logout, initAuth } from '../services/api';
import { DatePickerPopup } from './portal/shared/DatePickerPopup';
import { ViewType, FormData } from './portal/shared/types';
import { NavBar } from './portal/shared/NavBar';
import { HomePage } from './portal/HomePage';
import { ApplicationPage } from './portal/ApplicationPage';
import { LookupPage } from './portal/LookupPage';
import { AccountPage } from './portal/AccountPage';

// =============================================
// INITIAL FORM DATA
// =============================================
const initialFormData: FormData = {
  amount: '',
  term: '',
  fullName: '',
  dob: '',
  gender: '',
  purpose: '',
  email: '',
  phone: '',
  cccd: '',
  address: '',
  city: '',
  district: '',
  ward: '',
  occupation: '',
  income: '',
  ec1Name: '',
  ec1Relation: '',
  ec1Phone: '',
  ec2Name: '',
  ec2Relation: '',
  ec2Phone: '',
};

// =============================================
// CUSTOMER PORTAL (Shell)
// =============================================
export const CustomerPortal = () => {
  const [currentView, setCurrentView] = useState<ViewType>('LOGIN');
  const [formData, setFormData] = useState<FormData>(initialFormData);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Auth states
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');

  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regCccd, setRegCccd] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [regDob, setRegDob] = useState<Date | null>(null);
  const [showDobPicker, setShowDobPicker] = useState(false);
  const dobRef = useRef<HTMLDivElement>(null);
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const isLoggedIn = !['LOGIN', 'REGISTER'].includes(currentView);

  // Close dob picker when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dobRef.current && !dobRef.current.contains(e.target as Node)) {
        setShowDobPicker(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    // Silent refresh: nếu có refresh_token cookie hợp lệ, tự động khôi phục session
    const savedUser = localStorage.getItem('user');
    if (savedUser && savedUser !== 'undefined') {
      initAuth().then((result) => {
        // initAuth set accessToken vào memory nếu cookie hợp lệ
        // result là user info từ localStorage (có thể null nếu không có)
        // Nhưng nếu refresh thành công, token đã được set trong memory
        import('../services/api').then(({ getToken }) => {
          if (getToken()) {
            try {
              const parsed = JSON.parse(savedUser);
              setFormData(prev => ({
                ...prev,
                fullName: parsed.fullName || '',
                email: parsed.email || '',
                phone: parsed.phone || '',
                cccd: parsed.cccd || '',
                dob: parsed.dob || ''
              }));
              setCurrentView('HOME');
            } catch {
              logout();
            }
          } else {
            // Refresh token hết hạn → cần login lại
            logout();
          }
        });
      }).catch(() => logout());
    }
  }, []);


  const handleLogin = async () => {
    setErrorMsg('');
    if (!loginEmail || !loginPassword) {
      setErrorMsg('Vui lòng nhập tài khoản và mật khẩu.');
      return;
    }

    setIsLoading(true);
    try {
      const user = await loginAPI(loginEmail, loginPassword);
      localStorage.setItem('user', JSON.stringify(user));
      setFormData(prev => ({
        ...prev,
        fullName: user.fullName || '',
        email: user.email || '',
        phone: user.phone || '',
        cccd: user.cccd || '',
        dob: user.dob || ''
      }));
      setCurrentView('HOME');
    } catch (e: any) {
      setErrorMsg(e.message || 'Lỗi kết nối máy chủ.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async () => {
    setErrorMsg('');
    const isVnPhone = (p: string) => /^(0[35789])[0-9]{8}$/.test(p.trim());
    const isValidCccd = (c: string) => /^([0-9]{9}|[0-9]{12})$/.test(c.trim());
    const isValidEmail = (e: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e.trim());

    if (!regName || !regEmail || !regCccd || !regPassword || !regDob) {
      setErrorMsg('Vui lòng điền đủ các trường bắt buộc.');
      return;
    }

    if (!isValidEmail(regEmail)) {
      setErrorMsg('Email không hợp lệ.');
      return;
    }

    if (!isValidCccd(regCccd)) {
      setErrorMsg('CCCD/CMND không hợp lệ.');
      return;
    }

    if (regPhone && !isVnPhone(regPhone)) {
      setErrorMsg('Số điện thoại không hợp lệ.');
      return;
    }

    if (regPassword !== regConfirmPassword) {
      setErrorMsg('Mật khẩu không khớp.');
      return;
    }

    setIsLoading(true);
    try {
      const birthDate = regDob ? `${regDob.getFullYear()}-${String(regDob.getMonth() + 1).padStart(2, '0')}-${String(regDob.getDate()).padStart(2, '0')}` : undefined;
      await registerAPI({
        fullName: regName,
        email: regEmail,
        phone: regPhone || undefined,
        cccd: regCccd,
        password: regPassword,
        birthDate,
      });
      alert('Đăng ký thành công! Vui lòng đăng nhập.');
      setLoginEmail(regEmail);
      setCurrentView('LOGIN');
    } catch (e: any) {
      setErrorMsg(e.message || 'Lỗi kết nối máy chủ.');
    } finally {
      setIsLoading(false);
    }
  };

  // =============================================
  // LOGIN
  // =============================================
  const renderLogin = () => (
    <div className="min-h-screen bg-slate-50 flex flex-col lg:flex-row">
      {/* Left branding */}
      <div className="hidden lg:flex lg:w-5/12 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex-col justify-center items-center px-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, white 1px, transparent 0)', backgroundSize: '32px 32px' }} />
        <div className="relative z-10 max-w-sm">
          <div className="w-14 h-14 bg-blue-600 rounded-xl flex items-center justify-center mb-8">
            <ShieldCheck className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-3 leading-tight">Cổng Thông Tin<br />Khách Hàng</h1>
          <p className="text-slate-400 text-base leading-relaxed">Quản lý khoản vay, theo dõi hồ sơ và trải nghiệm dịch vụ tài chính một cách dễ dàng và an toàn.</p>
        </div>
      </div>

      {/* Right form */}
      <div className="flex-1 flex flex-col justify-center px-6 sm:px-12 lg:px-16 xl:px-24 py-12">
        <div className="max-w-sm w-full mx-auto lg:mx-0">
          <div className="lg:hidden flex items-center gap-2.5 mb-10">
            <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center">
              <ShieldCheck className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-slate-800 text-lg">Cổng Khách Hàng</span>
          </div>

          <h2 className="text-2xl font-bold text-slate-800 mb-1">Đăng nhập</h2>
          <p className="text-slate-500 text-sm mb-8">Nhập thông tin tài khoản để tiếp tục</p>

          {errorMsg && (
            <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-100">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Email hoặc số điện thoại</label>
              <input type="text" placeholder="Nhập tài khoản" value={loginEmail} onChange={e => setLoginEmail(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-sm font-medium text-slate-700">Mật khẩu</label>
                <span className="text-xs text-blue-600 font-medium cursor-pointer hover:underline">Quên mật khẩu?</span>
              </div>
              <input type="password" placeholder="Nhập mật khẩu" value={loginPassword} onChange={e => setLoginPassword(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <button disabled={isLoading} onClick={handleLogin} className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg shadow-sm transition flex justify-center items-center gap-2 cursor-pointer text-sm mt-2">
              {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              Đăng nhập
            </button>
          </div>

          <p className="mt-6 text-center text-sm text-slate-500">
            Chưa có tài khoản?{' '}
            <span onClick={() => { setCurrentView('REGISTER'); setErrorMsg(''); }} className="text-blue-600 font-semibold cursor-pointer hover:underline">Đăng ký</span>
          </p>
        </div>
      </div>
    </div>
  );

  // =============================================
  // REGISTER
  // =============================================
  const renderRegister = () => (
    <div className="min-h-screen bg-slate-50 flex flex-col lg:flex-row">
      <div className="hidden lg:flex lg:w-5/12 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex-col justify-center items-center px-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, white 1px, transparent 0)', backgroundSize: '32px 32px' }} />
        <div className="relative z-10 max-w-sm">
          <div className="w-14 h-14 bg-blue-600 rounded-xl flex items-center justify-center mb-8">
            <ShieldCheck className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-3 leading-tight">Tạo Tài Khoản</h1>
          <p className="text-slate-400 text-base leading-relaxed">Chỉ mất vài phút để đăng ký và bắt đầu trải nghiệm các dịch vụ tài chính.</p>
        </div>
      </div>

      <div className="flex-1 flex flex-col justify-center px-6 sm:px-12 lg:px-16 xl:px-24 py-12">
        <div className="max-w-sm w-full mx-auto lg:mx-0">
          <div className="lg:hidden flex items-center gap-2.5 mb-10">
            <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center">
              <ShieldCheck className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-slate-800 text-lg">Cổng Khách Hàng</span>
          </div>

          <div className="flex items-center gap-3 mb-6">
            <button onClick={() => { setCurrentView('LOGIN'); setErrorMsg(''); }} className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition cursor-pointer lg:hidden">
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div>
              <h2 className="text-2xl font-bold text-slate-800">Đăng ký</h2>
              <p className="text-slate-500 text-sm">Tạo tài khoản mới</p>
            </div>
          </div>

          {errorMsg && (
            <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-100">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Họ và tên *</label>
              <input type="text" placeholder="Nhập họ và tên" value={regName} onChange={e => setRegName(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Email *</label>
              <input type="text" placeholder="Nhập địa chỉ email" value={regEmail} onChange={e => setRegEmail(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Ngày sinh *</label>
              <div ref={dobRef} className="relative">
                <button
                  type="button"
                  onClick={() => setShowDobPicker(v => !v)}
                  className="w-full flex items-center justify-between border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white cursor-pointer hover:border-blue-400"
                >
                  <span className={regDob ? 'text-slate-800' : 'text-slate-400'}>
                    {regDob ? `${String(regDob.getDate()).padStart(2, '0')}/${String(regDob.getMonth() + 1).padStart(2, '0')}/${regDob.getFullYear()}` : 'Chọn ngày sinh'}
                  </span>
                  <CalendarDays className="w-4 h-4 text-slate-400" />
                </button>
                {showDobPicker && (
                  <div className="absolute z-50 mt-1">
                    <DatePickerPopup
                      value={regDob}
                      onChange={(date) => setRegDob(date)}
                      onClose={() => setShowDobPicker(false)}
                    />
                  </div>
                )}
              </div>
            </div>
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="text-sm font-medium text-slate-700 mb-1.5 block">Số điện thoại</label>
                <input type="text" placeholder="Nhập SĐT" value={regPhone} onChange={e => setRegPhone(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
              </div>
              <div className="flex-1">
                <label className="text-sm font-medium text-slate-700 mb-1.5 block">Số CCCD *</label>
                <input type="text" placeholder="Nhập CCCD" value={regCccd} onChange={e => setRegCccd(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Mật khẩu *</label>
              <input type="password" placeholder="Tạo mật khẩu" value={regPassword} onChange={e => setRegPassword(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Xác nhận mật khẩu *</label>
              <input type="password" placeholder="Nhập lại mật khẩu" value={regConfirmPassword} onChange={e => setRegConfirmPassword(e.target.value)} className="w-full border border-slate-300 text-slate-800 rounded-lg px-3.5 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition placeholder:text-slate-400" />
            </div>
            <button disabled={isLoading} onClick={handleRegister} className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 flex justify-center items-center gap-2 text-white font-semibold py-2.5 rounded-lg shadow-sm transition cursor-pointer text-sm mt-2">
              {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              Tạo tài khoản
            </button>
          </div>

          <p className="mt-6 text-center text-sm text-slate-500">
            Đã có tài khoản?{' '}
            <span onClick={() => { setCurrentView('LOGIN'); setErrorMsg(''); }} className="text-blue-600 font-semibold cursor-pointer hover:underline">Đăng nhập</span>
          </p>
        </div>
      </div>
    </div>
  );

  // =============================================
  // MAIN RENDER
  // =============================================
  return (
    <div className="w-full min-h-screen bg-slate-50 font-sans text-slate-800 antialiased">
      {isLoggedIn && (
        <NavBar
          currentView={currentView}
          setCurrentView={setCurrentView}
          fullName={formData.fullName}
          mobileMenuOpen={mobileMenuOpen}
          setMobileMenuOpen={setMobileMenuOpen}
        />
      )}
      <main>
        {currentView === 'LOGIN' && renderLogin()}
        {currentView === 'REGISTER' && renderRegister()}
        {currentView === 'HOME' && (
          <HomePage formData={formData} setCurrentView={setCurrentView} setFormData={setFormData} />
        )}
        {(currentView === 'APPLY' || currentView === 'CONFIRM' || currentView === 'SUCCESS') && (
          <ApplicationPage formData={formData} setFormData={setFormData} setCurrentView={setCurrentView} />
        )}
        {currentView === 'HISTORY' && (
          <LookupPage formData={formData} setCurrentView={setCurrentView} />
        )}
        {currentView === 'PROFILE' && (
          <AccountPage formData={formData} setCurrentView={setCurrentView} />
        )}
      </main>
    </div>
  );
};
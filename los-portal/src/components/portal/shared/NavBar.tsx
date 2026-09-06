import { FileText, User, Bell, CreditCard, ShieldCheck, LayoutDashboard, Menu, X } from 'lucide-react';
import { ViewType } from './types';

interface NavBarProps {
  currentView: ViewType;
  setCurrentView: (v: ViewType) => void;
  fullName: string;
  mobileMenuOpen: boolean;
  setMobileMenuOpen: (v: boolean) => void;
}

const navLinks = [
  { key: 'HOME' as ViewType, label: 'Tổng quan', icon: LayoutDashboard },
  { key: 'HISTORY' as ViewType, label: 'Hồ sơ vay', icon: FileText },
  { key: 'PROFILE' as ViewType, label: 'Tài khoản', icon: User },
];

export const NavBar = ({ currentView, setCurrentView, fullName, mobileMenuOpen, setMobileMenuOpen }: NavBarProps) => {
  const isActive = (key: ViewType) =>
    currentView === key || (key === 'HOME' && ['HOME', 'APPLY', 'CONFIRM', 'SUCCESS'].includes(currentView));

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14">
          {/* Logo */}
          <div className="flex items-center gap-2.5 cursor-pointer select-none" onClick={() => setCurrentView('HOME')}>
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <ShieldCheck className="w-4 h-4 text-white" />
            </div>
            <span className="font-bold text-slate-800 text-base hidden sm:block">Cổng Khách Hàng</span>
          </div>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map(link => {
              const Icon = link.icon;
              return (
                <button
                  key={link.key}
                  onClick={() => setCurrentView(link.key)}
                  className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-sm font-medium transition cursor-pointer ${isActive(link.key) ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-800'}`}
                >
                  <Icon className="w-4 h-4" />
                  {link.label}
                </button>
              );
            })}
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-3">
            <button className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition cursor-pointer">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full" />
            </button>
            <div className="hidden sm:flex items-center gap-2 pl-3 border-l border-slate-200">
              <div className="w-8 h-8 bg-slate-200 rounded-full flex items-center justify-center">
                <User className="w-4 h-4 text-slate-600" />
              </div>
              <span className="text-sm font-medium text-slate-700 hidden lg:block">{fullName}</span>
            </div>
            {/* Mobile hamburger */}
            <button className="md:hidden p-2 text-slate-500 hover:bg-slate-100 rounded-lg cursor-pointer" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-100 bg-white px-4 pb-3 pt-2 space-y-1">
          {navLinks.map(link => {
            const Icon = link.icon;
            return (
              <button
                key={link.key}
                onClick={() => { setCurrentView(link.key); setMobileMenuOpen(false); }}
                className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium transition cursor-pointer ${isActive(link.key) ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-50'}`}
              >
                <Icon className="w-4 h-4" />
                {link.label}
              </button>
            );
          })}
          <div className="border-t border-slate-100 pt-2 mt-2">
            <button
              onClick={() => { localStorage.removeItem('user'); setCurrentView('LOGIN'); setMobileMenuOpen(false); }}
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 transition cursor-pointer"
            >
              <CreditCard className="w-4 h-4" />
              Đăng xuất
            </button>
          </div>
        </div>
      )}
    </header>
  );
};

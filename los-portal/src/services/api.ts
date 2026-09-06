/**
 * api.ts — Central API service for Spring Boot LOS backend.
 * Uses Access/Refresh Token pattern:
 *  - Access token: stored in memory (never localStorage), 15 min TTL
 *  - Refresh token: HttpOnly cookie (set by server), 7 day TTL
 *  - On 401: auto-retry once via POST /auth/refresh
 *  - On app startup: call initAuth() to silently restore session
 */

const API_BASE = '/api/v1';

// ─── In-Memory Token Store ──────────────────────────────────
// Access token lives only in JS memory — safe from XSS, but lost on page reload.
// Page reload → initAuth() silently calls /auth/refresh using HttpOnly cookie.

let _accessToken: string | null = null;
let _isRefreshing = false;
let _refreshQueue: Array<(token: string | null) => void> = [];

export function getToken(): string | null {
  return _accessToken;
}

export function setToken(token: string | null): void {
  _accessToken = token;
}

export function clearToken(): void {
  _accessToken = null;
}

// ─── Auth User type (declared early for use in initAuth) ────

export interface AuthUser {
  accessToken?: string;
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  cccd: string;
  dob: string;
}


// ─── ApiResponse shape ──────────────────────────────────────

interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
}

// ─── Core Fetch Helper ──────────────────────────────────────

async function apiFetch<T = any>(
  endpoint: string,
  options: RequestInit = {},
  _retry = true
): Promise<ApiResponse<T>> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
    credentials: 'include', // send cookies (refresh_token HttpOnly cookie)
  });

  // ── Auto-refresh on 401 ───────────────────────────────────
  if (res.status === 401 && _retry) {
    const newToken = await _doRefresh();
    if (newToken) {
      // Retry original request with new token
      return apiFetch<T>(endpoint, options, false);
    } else {
      // Refresh failed → force logout
      _onSessionExpired();
      throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }
  }

  let json: any = {};
  try {
    json = await res.json();
  } catch {
    json = { success: res.ok, message: `HTTP ${res.status}` };
  }

  if (!res.ok || !json.success) {
    let errorMsg = json.message || `HTTP ${res.status}`;
    if (json.data && typeof json.data === 'object') {
      const details = Object.values(json.data).join(', ');
      if (details) errorMsg += `: ${details}`;
    }
    throw new Error(errorMsg);
  }

  return json;
}

// ─── Refresh Logic ──────────────────────────────────────────

/**
 * Silently refresh access token using HttpOnly refresh_token cookie.
 * Queues concurrent requests to avoid multiple simultaneous refresh calls.
 */
async function _doRefresh(): Promise<string | null> {
  if (_isRefreshing) {
    // Wait for the in-progress refresh
    return new Promise((resolve) => {
      _refreshQueue.push(resolve);
    });
  }

  _isRefreshing = true;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include', // send HttpOnly cookie
    });

    if (!res.ok) {
      _processRefreshQueue(null);
      return null;
    }

    const json = await res.json();
    const newToken: string | null = json?.data?.accessToken ?? null;

    setToken(newToken);
    _processRefreshQueue(newToken);
    return newToken;
  } catch {
    _processRefreshQueue(null);
    return null;
  } finally {
    _isRefreshing = false;
  }
}

function _processRefreshQueue(token: string | null): void {
  _refreshQueue.forEach((resolve) => resolve(token));
  _refreshQueue = [];
}

function _onSessionExpired(): void {
  clearToken();
  localStorage.removeItem('user');
  // Redirect to login — dùng hard reload để clear React state
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/';
  }
}

// ─── initAuth — gọi khi app khởi động (silent refresh) ─────

/**
 * Gọi 1 lần khi app mount. Nếu có refresh_token cookie hợp lệ,
 * tự động lấy access token mới mà không cần user login lại.
 * Returns: user info nếu thành công, null nếu không có session.
 */
export async function initAuth(): Promise<AuthUser | null> {
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });

    if (!res.ok) return null;

    const json = await res.json();
    if (!json?.success || !json?.data?.accessToken) return null;

    setToken(json.data.accessToken);

    // Lấy lại thông tin user từ localStorage nếu có
    try {
      const saved = localStorage.getItem('user');
      if (saved) return JSON.parse(saved) as AuthUser;
    } catch {}

    return null;
  } catch {
    return null;
  }
}




export async function loginAPI(email: string, password: string): Promise<AuthUser> {
  const res = await apiFetch<AuthUser>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });

  if (!res.success || !res.data) {
    throw new Error(res.message || 'Đăng nhập thất bại.');
  }

  // Lưu access token vào memory
  if (res.data.accessToken) {
    setToken(res.data.accessToken);
  }

  // Lưu thông tin user vào localStorage (không lưu token)
  const userInfo: AuthUser = { ...res.data, accessToken: undefined };
  localStorage.setItem('user', JSON.stringify(userInfo));

  return userInfo;
}

export async function registerAPI(params: {
  fullName: string;
  email: string;
  cccd: string;
  password: string;
  phone?: string;
  birthDate?: string;
}): Promise<AuthUser> {
  const res = await apiFetch<AuthUser>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(params),
  });

  if (!res.success || !res.data) {
    throw new Error(res.message || 'Đăng ký thất bại.');
  }

  return res.data;
}

export async function logout(): Promise<void> {
  try {
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch {
    // best effort
  } finally {
    clearToken();
    localStorage.removeItem('user');
  }
}

// ─── OTP ─────────────────────────────────────────────────────

export async function sendOtpAPI(email: string): Promise<void> {
  const res = await apiFetch<void>('/otp/send', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });

  if (!res.success) {
    throw new Error(res.message || 'Có lỗi khi gửi OTP.');
  }
}

export async function verifyOtpAPI(email: string, otp: string): Promise<void> {
  const res = await apiFetch<void>('/otp/verify', {
    method: 'POST',
    body: JSON.stringify({ email, otp }),
  });

  if (!res.success) {
    throw new Error(res.message || 'Mã OTP không hợp lệ.');
  }
}

// ─── Loan Application ────────────────────────────────────────

export interface LoanSubmitParams {
  loanProductId?: string;
  amount: number;
  term: number;
  fullName: string;
  cccd: string;
  email: string;
  phoneNumber?: string;
  gender?: string;
  birthDate?: string;
  address?: string;
  occupation?: string;
  incomeRange?: string;
  purpose?: string;
  ref1Name?: string;
  ref1Phone?: string;
  ref2Name?: string;
  ref2Phone?: string;
}

export interface LoanRecord {
  loanApplicationId: string;
  status: string;
  amount: number;
  term: number;
  interestRate: number;
  purpose: string;
  submittedAt: string;
  fullName: string;
  cccd: string;
  email: string;
}

export async function submitLoanAPI(params: LoanSubmitParams): Promise<any> {
  const res = await apiFetch('/portal/submit', {
    method: 'POST',
    body: JSON.stringify(params),
  });

  if (!res.success) {
    throw new Error(res.message || 'Có lỗi xảy ra khi nộp hồ sơ.');
  }

  return res.data;
}

export async function getLoanHistoryAPI(
  cccd: string,
  fromDate?: string,
  toDate?: string
): Promise<LoanRecord[]> {
  const queryParams = new URLSearchParams();
  if (fromDate) queryParams.append('fromDate', fromDate);
  if (toDate) queryParams.append('toDate', toDate);
  const qs = queryParams.toString() ? `?${queryParams.toString()}` : '';

  const res = await apiFetch<LoanRecord[]>(`/portal/history/${encodeURIComponent(cccd)}${qs}`);

  if (!res.success) {
    throw new Error(res.message || 'Lỗi khi tải lịch sử.');
  }

  return res.data || [];
}

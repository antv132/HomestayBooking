import api from './axiosInstance';

// ── Types ─────────────────────────────────────────────────────────────────────

export type Role = 'CUSTOMER' | 'MANAGER';

/** Thông tin user trong response đăng nhập — khớp với backend AuthResponse.UserInfo */
export interface UserInfo {
  id:        string;
  fullName:  string;
  email:     string;
  phone?:    string;
  avatarUrl?: string;
  role:      string;   // 'CUSTOMER' | 'MANAGER'
  status:    string;   // 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
}

/** Full session after login / refresh / Google complete-link */
export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
}

/** Response đăng nhập / Google — có thể pending OTP liên kết (FR-006) */
export interface AuthResponse extends Partial<AuthSession> {
  pendingVerification?: boolean;
  verificationToken?: string | null;
  email?: string | null;
}

export function isAuthSession(data: AuthResponse): data is AuthSession {
  return !!(data.accessToken && data.refreshToken && data.user);
}

export interface ApiResponse<T = null> {
  success: boolean;
  message: string;
  data:    T;
}

export interface RegisterRequest {
  fullName: string;
  email:    string;
  password: string;
  phone:    string;
}

export interface OtpVerifyRequest {
  email:   string;
  otpCode: string;
}

// ── Auth API ──────────────────────────────────────────────────────────────────

export const authApi = {
  login: async (email: string, password: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await api.post('/api/v1/auth/login', { email, password });
    return res.data;
  },

  // Đăng ký tài khoản mới (mặc định role CUSTOMER)
  register: async (payload: RegisterRequest): Promise<ApiResponse> => {
    const res = await api.post('/api/v1/auth/register', payload);
    return res.data;
  },

  // Xác thực OTP sau khi đăng ký
  verifyOtp: async (req: OtpVerifyRequest): Promise<ApiResponse> => {
    const res = await api.post('/api/v1/auth/verify-otp', req);
    return res.data;
  },

  // Gửi lại OTP
  resendOtp: async (email: string): Promise<ApiResponse> => {
    const res = await api.post('/api/v1/auth/resend-otp', { email });
    return res.data;
  },

  // Đăng nhập bằng Google (ID Token từ frontend)
  loginWithGoogle: async (idToken: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await api.post('/api/v1/auth/google', { idToken });
    return res.data;
  },

  // FR-006: hoàn tất liên kết Google sau OTP
  completeGoogleLink: async (verificationToken: string, otpCode: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await api.post('/api/v1/auth/google/complete-link', { verificationToken, otpCode });
    return res.data;
  },

  // Refresh access token — gửi JSON object { refreshToken } theo backend RefreshTokenRequest
  refreshToken: async (refreshToken: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await api.post('/api/v1/auth/refresh', { refreshToken });
    return res.data;
  },

  // Đăng xuất — gửi JSON object { refreshToken } theo backend RefreshTokenRequest
  logout: async (refreshToken: string): Promise<ApiResponse<void>> => {
    const res = await api.post('/api/v1/auth/logout', { refreshToken });
    return res.data;
  },

  // Quên mật khẩu
  forgotPassword: async (email: string): Promise<ApiResponse> => {
    const res = await api.post('/api/v1/auth/forgot-password', { email });
    return res.data;
  },

  // Đặt lại mật khẩu — backend ResetPasswordRequest cần email + otpCode + newPassword
  resetPassword: async (email: string, otpCode: string, newPassword: string): Promise<ApiResponse> => {
    const res = await api.post('/api/v1/auth/reset-password', { email, otpCode, newPassword });
    return res.data;
  },
};

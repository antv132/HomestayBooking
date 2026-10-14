import { create } from 'zustand';
import { AuthSession } from '../api/authApi';

// Trạng thái xác thực người dùng — lưu vào sessionStorage (xóa khi đóng browser)
interface AuthState {
  isAuthenticated: boolean;
  role:      string | null;  // 'CUSTOMER' | 'MANAGER'
  userId:    string | null;
  fullName:  string | null;
  email:     string | null;
  phone:     string | null;
  avatarUrl: string | null;

  login:         (data: AuthSession) => void;
  logout:        () => void;
  updateProfile: (data: { fullName?: string; avatarUrl?: string; phone?: string }) => void;
}

const STORAGE_KEYS = ['accessToken', 'refreshToken', 'userRole', 'userId', 'fullName', 'userEmail', 'userPhone', 'avatarUrl'] as const;

export function getRoleDashboardPath(role: string | null | undefined): string {
  switch (role) {
    case 'MANAGER':
      return '/manager/dashboard';
    case 'ADMIN':
      return '/admin/dashboard';
    case 'EMPLOYEE':
      return '/employee/dashboard';
    default:
      return '/customer/dashboard';
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  // Khôi phục state từ sessionStorage khi reload trang (giữ phiên trong cùng tab)
  isAuthenticated: !!sessionStorage.getItem('accessToken'),
  role:      sessionStorage.getItem('userRole'),
  userId:    sessionStorage.getItem('userId'),
  fullName:  sessionStorage.getItem('fullName'),
  email:     sessionStorage.getItem('userEmail'),
  phone:     sessionStorage.getItem('userPhone'),
  avatarUrl: sessionStorage.getItem('avatarUrl'),

  login: (data: AuthSession) => {
    // Persist tokens và thông tin user vào sessionStorage (bị xóa khi đóng browser)
    sessionStorage.setItem('accessToken',  data.accessToken);
    sessionStorage.setItem('refreshToken', data.refreshToken);
    sessionStorage.setItem('userRole',     data.user.role);
    sessionStorage.setItem('userId',       data.user.id       ?? '');
    sessionStorage.setItem('fullName',     data.user.fullName ?? '');
    sessionStorage.setItem('userEmail',    data.user.email    ?? '');
    sessionStorage.setItem('userPhone',    data.user.phone    ?? '');
    sessionStorage.setItem('avatarUrl',    data.user.avatarUrl ?? '');

    set({
      isAuthenticated: true,
      role:      data.user.role,
      userId:    data.user.id        ?? null,
      fullName:  data.user.fullName  ?? null,
      email:     data.user.email     ?? null,
      phone:     data.user.phone     ?? null,
      avatarUrl: data.user.avatarUrl ?? null,
    });
  },

  logout: () => {
    STORAGE_KEYS.forEach((k) => sessionStorage.removeItem(k));
    // Dọn sạch cả localStorage phòng trường hợp còn sót từ version cũ
    STORAGE_KEYS.forEach((k) => localStorage.removeItem(k));
    set({
      isAuthenticated: false,
      role:      null,
      userId:    null,
      fullName:  null,
      email:     null,
      phone:     null,
      avatarUrl: null,
    });
  },

  // Cập nhật profile mà không cần logout/login lại
  updateProfile: (data) => {
    if (data.fullName  !== undefined) sessionStorage.setItem('fullName',  data.fullName);
    if (data.avatarUrl !== undefined) sessionStorage.setItem('avatarUrl', data.avatarUrl);
    if (data.phone     !== undefined) sessionStorage.setItem('userPhone', data.phone);

    set((prev) => ({
      fullName:  data.fullName  !== undefined ? data.fullName  : prev.fullName,
      avatarUrl: data.avatarUrl !== undefined ? data.avatarUrl : prev.avatarUrl,
      phone:     data.phone     !== undefined ? data.phone     : prev.phone,
    }));
  },
}));

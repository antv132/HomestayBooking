import axios from 'axios';
import { authApi, isAuthSession } from './authApi';
import { useAuthStore } from '../store/authStore';

const PUBLIC_READ_PATHS = [
  '/api/rooms',
  '/api/properties',
  '/api/public',
  '/api/v1/promotions',
  '/api/v1/public',
  '/api/v1/properties',
  '/api/v1/rooms',
  '/uploads',
];

function isPublicReadRoute(url?: string) {
  if (!url) return false;
  return PUBLIC_READ_PATHS.some((p) => url.includes(p));
}

const api = axios.create({
  // Dev: dùng Vite proxy (/api → localhost:8080). Prod: set VITE_API_URL.
  baseURL: import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? '' : 'http://localhost:8080'),
  timeout: 10000,
});

// Gắn Access Token vào mọi request
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Xử lý 401 — tự động refresh token rồi retry request gốc
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    const isUnauthorized = error.response?.status === 401
                        || (error.response?.status === 403 && !isPublicReadRoute(originalRequest.url));
    const isNotRetried   = !originalRequest._retry;
    const isNotAuthRoute = !originalRequest.url?.includes('/api/auth/login')
      && !originalRequest.url?.includes('/api/auth/refresh');

    if (isUnauthorized && isNotRetried && isNotAuthRoute) {
      originalRequest._retry = true;

      // Public GET routes: stale token must not block browsing or redirect to login
      if (isPublicReadRoute(originalRequest.url) && originalRequest.headers.Authorization) {
        delete originalRequest.headers.Authorization;
        return api(originalRequest);
      }

      const refreshToken = sessionStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const res = await authApi.refreshToken(refreshToken);

          if (res.success && res.data && isAuthSession(res.data)) {
            // Cập nhật tokens trong sessionStorage
            sessionStorage.setItem('accessToken', res.data.accessToken);
            sessionStorage.setItem('refreshToken', res.data.refreshToken);

            // Đồng bộ Zustand store với data mới
            useAuthStore.getState().login(res.data);

            // Retry request gốc với token mới
            originalRequest.headers.Authorization = `Bearer ${res.data.accessToken}`;
            return api(originalRequest);
          }
        } catch {
          // Refresh thất bại — đăng xuất và redirect
        }
      }

      // Không có refresh token hoặc refresh thất bại → logout (trừ trang public)
      if (!isPublicReadRoute(originalRequest.url)) {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default api;
